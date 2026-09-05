#!/usr/bin/env bash
set -uo pipefail

PORT_OFFSET="${SMOKE_PORT_OFFSET:-10000}"
export COMERCIAL_URL="http://localhost:$((8010 + PORT_OFFSET))"
export PROGRAMACION_URL="http://localhost:$((8020 + PORT_OFFSET))"
export EJECUCION_URL="http://localhost:$((8030 + PORT_OFFSET))"
export UNIDADES_URL="http://localhost:$((8040 + PORT_OFFSET))"
export CONDUCTORES_URL="http://localhost:$((8050 + PORT_OFFSET))"
export FACTURACION_URL="http://localhost:$((8060 + PORT_OFFSET))"
export COBRANZA_URL="http://localhost:$((8070 + PORT_OFFSET))"

paso() {
    local desc="$1"
    local method="$2"
    local url="$3"
    local body="$4"
    local expected="$5"
    local tmp_body=$(mktemp)
    
    if [ "$body" != "null" ]; then
        http_code=$(curl -s -X "$method" "$url" -H "Content-Type: application/json" -d "$body" -w "%{http_code}" -o "$tmp_body")
    else
        http_code=$(curl -s -X "$method" "$url" -w "%{http_code}" -o "$tmp_body")
    fi
    
    if [ $? -ne 0 ] || [ -z "$http_code" ] || [ "$http_code" = "000" ]; then
        echo "Error de conexion: hay que levantar los servicios con docker compose up -d"
        exit 1
    fi

    if [ "$http_code" != "$expected" ]; then
        echo "$desc"
        echo "Estado recibido: $http_code"
        echo "Estado esperado: $expected"
        cat "$tmp_body"
        echo ""
        exit 1
    fi
    echo "  ok  $desc"
    cat "$tmp_body" > .latest_response.json
}

# 1. Comercial: Registrar cliente
paso "Registrar cliente" "POST" "$COMERCIAL_URL/api/v1/clientes" '{"ruc":"20123456789","razonSocial":"Cliente Humo","modalidadDePago":"CREDITO","plazoEnDias":30}' "201"
CLIENTE_ID=$(python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" < .latest_response.json)

# 2. Comercial: Registrar contrato marco
paso "Registrar contrato marco" "POST" "$COMERCIAL_URL/api/v1/contratos-marco" "{\"clienteId\":\"$CLIENTE_ID\",\"vigenteDesde\":\"2020-01-01\",\"vigenteHasta\":\"2030-12-31\",\"tiempoLibreHoras\":2,\"consolidacionPermitida\":true,\"consolidacionRestricciones\":[],\"tarifasPactadas\":[{\"rutaOrigen\":\"Lima\",\"rutaDestino\":\"Piura\",\"rutaCorredor\":\"Norte\",\"tipoUnidad\":\"FURGON\",\"precioMonto\":1500,\"precioMoneda\":\"PEN\"}]}" "201"
CONTRATO_ID=$(python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" < .latest_response.json)

# 3. Unidades: Registrar unidad y SOAT (documento)
paso "Registrar unidad" "POST" "$UNIDADES_URL/api/v1/unidades" '{"placa":"A1B-234","tipo":"FURGON","pesoMaximoKg":15000,"volumenMaximoM3":30.0,"kilometraje":0,"intervaloMantenimiento":"ACEITE_Y_FILTROS"}' "201"
UNIDAD_ID=$(python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" < .latest_response.json)
paso "Documento SOAT" "POST" "$UNIDADES_URL/api/v1/unidades/$UNIDAD_ID/documentos" '{"tipoDocumento":"SOAT","desde":"2020-01-01","hasta":"2030-01-01","numero":"12345"}' "201"

# 3. Conductores: Registrar conductores e inducciones (para cliente)
paso "Registrar conductor 1" "POST" "$CONDUCTORES_URL/api/v1/conductores" '{"nombreCompleto":"Juan Perez","numeroDeLicencia":"Q12345678","categoriaDeLicencia":"A_IIIC","licenciaDesde":"2020-01-01","licenciaHasta":"2030-01-01"}' "201"
COND1_ID=$(python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" < .latest_response.json)
paso "Induccion cond1" "POST" "$CONDUCTORES_URL/api/v1/conductores/$COND1_ID/inducciones" "{\"clienteId\":\"$CLIENTE_ID\",\"vigenteDesde\":\"2020-01-01\",\"vigenteHasta\":\"2030-01-01\"}" "201"

paso "Registrar conductor 2" "POST" "$CONDUCTORES_URL/api/v1/conductores" '{"nombreCompleto":"Pedro Perez","numeroDeLicencia":"Q87654321","categoriaDeLicencia":"A_IIIC","licenciaDesde":"2020-01-01","licenciaHasta":"2030-01-01"}' "201"
COND2_ID=$(python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" < .latest_response.json)
paso "Induccion cond2" "POST" "$CONDUCTORES_URL/api/v1/conductores/$COND2_ID/inducciones" "{\"clienteId\":\"$CLIENTE_ID\",\"vigenteDesde\":\"2020-01-01\",\"vigenteHasta\":\"2030-01-01\"}" "201"

# 4. Comercial: Crear orden de servicio a CREDITO
paso "Crear orden" "POST" "$COMERCIAL_URL/api/v1/ordenes" "{\"clienteId\":\"$CLIENTE_ID\",\"contratoId\":\"$CONTRATO_ID\",\"tipoUnidad\":\"FURGON\",\"cargaPesoKg\":10000,\"cargaVolumenM3\":25.0,\"cargaTipo\":\"GENERAL\",\"rutaOrigen\":\"Lima\",\"rutaDestino\":\"Piura\",\"rutaCorredor\":\"Norte\",\"cargaEmbalaje\":\"CAJAS\",\"cargaNaturaleza\":\"SECA\",\"rutaDistanciaKm\":1000,\"ventanaInicio\":\"2026-10-10T08:00:00-05:00\",\"ventanaFin\":\"2026-10-10T18:00:00-05:00\",\"modalidadDePago\":\"CREDITO\",\"plazoEnDias\":30}" "201"
ORDEN_ID=$(python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" < .latest_response.json)

# 5. Comercial: Confirmar orden
paso "Confirmar orden" "POST" "$COMERCIAL_URL/api/v1/ordenes/$ORDEN_ID/confirmar" "null" "200"

# 6. Programacion: Crear viaje, consolidar y asignar recursos, programar y despachar
# NOTA: En la API de Programación, el contrato 1 se ejerce internamente dentro del servicio en dos lugares:
# 1) `consolidarOrden` (POST /{id}/ordenes), que es para consolidar una carga *adicional*.
# 2) `asignarRecursos` (POST /{id}/recursos), que verifica los requisitos de la unidad y del cliente contra las órdenes.
# Como el flujo sólo define una orden de carga en el paso 4, la agregamos directo en "planificar" y 
# usamos "asignarRecursos" (que llama al contrato 1 internamente para verificar al cliente y unidad requerida),
# evitando consolidar la misma orden por segunda vez, lo que duplicaría el volumen.
VIAJE_ID="VIA-1000"
paso "Planificar viaje" "POST" "$PROGRAMACION_URL/api/v1/viajes" "{\"id\":\"$VIAJE_ID\",\"ruta\":{\"origen\":\"Lima\",\"destino\":\"Piura\",\"corredor\":\"Norte\"},\"ventana\":{\"desde\":\"2026-10-10T08:00:00-05:00\",\"hasta\":\"2026-10-10T18:00:00-05:00\"},\"cargaInicial\":{\"ordenDeServicioId\":\"$ORDEN_ID\",\"pesoKg\":10000,\"volumenM3\":25.0,\"tipo\":\"GENERAL\",\"secuenciaDeDescarga\":1}}" "201"

paso "Asignar recursos" "POST" "$PROGRAMACION_URL/api/v1/viajes/$VIAJE_ID/recursos" "{\"unidadId\":\"$UNIDAD_ID\",\"conductorIds\":[\"$COND1_ID\",\"$COND2_ID\"],\"conRelevo\":true}" "200"

paso "Programar viaje" "POST" "$PROGRAMACION_URL/api/v1/viajes/$VIAJE_ID/programar" "{\"hojaDeRuta\":[{\"secuencia\":1,\"tipo\":\"CARGA\",\"ordenDeServicioId\":\"$ORDEN_ID\",\"ubicacion\":{\"direccion\":\"Lima 123\"},\"horaEstimada\":\"2026-10-10T09:00:00-05:00\"},{\"secuencia\":2,\"tipo\":\"DESCARGA\",\"ordenDeServicioId\":\"$ORDEN_ID\",\"ubicacion\":{\"direccion\":\"Piura 456\"},\"horaEstimada\":\"2026-10-10T17:00:00-05:00\"}],\"observaciones\":\"Cuidado\"}" "200"

paso "Despachar viaje" "POST" "$PROGRAMACION_URL/api/v1/viajes/$VIAJE_ID/despachar" "null" "200"

# 7. Ejecucion: check-list, iniciar, conformidades, cerrar
paso "Crear ejecucion" "POST" "$EJECUCION_URL/api/v1/ejecuciones" "{\"viajeId\":\"$VIAJE_ID\"}" "201"
paso "Registrar checklist" "POST" "$EJECUCION_URL/api/v1/ejecuciones/$VIAJE_ID/checklist" "{\"aprobado\":true,\"observaciones\":\"Todo bien\"}" "200"
paso "Iniciar ejecucion" "POST" "$EJECUCION_URL/api/v1/ejecuciones/$VIAJE_ID/iniciar" "null" "200"
paso "Conformidad parada 1" "POST" "$EJECUCION_URL/api/v1/ejecuciones/$VIAJE_ID/paradas/1/conformidad" "{\"estado\":\"FIRMADA\",\"recibidoPor\":\"Encargado 1\"}" "201"
paso "Conformidad parada 2" "POST" "$EJECUCION_URL/api/v1/ejecuciones/$VIAJE_ID/paradas/2/conformidad" "{\"estado\":\"FIRMADA\",\"recibidoPor\":\"Encargado 2\"}" "201"
paso "Cerrar ejecucion" "POST" "$EJECUCION_URL/api/v1/ejecuciones/$VIAJE_ID/cerrar" "{\"kilometrajeFinal\":1000,\"horasPorConductor\":[{\"conductorId\":\"$COND1_ID\",\"horas\":4,\"desde\":\"2026-10-10T09:00:00-05:00\",\"hasta\":\"2026-10-10T13:00:00-05:00\"},{\"conductorId\":\"$COND2_ID\",\"horas\":4,\"desde\":\"2026-10-10T13:00:00-05:00\",\"hasta\":\"2026-10-10T17:00:00-05:00\"}],\"conceptosFacturables\":[{\"ordenDeServicioId\":\"$ORDEN_ID\",\"concepto\":\"ESTIBA\",\"monto\":\"1500.00\",\"moneda\":\"PEN\"}]}" "200"

# 8. Facturacion: factura y emision
paso "Abrir factura" "POST" "$FACTURACION_URL/api/v1/facturas" "{\"ordenDeServicioId\":\"$ORDEN_ID\",\"clienteId\":\"$CLIENTE_ID\",\"snapshot\":{\"tarifaMonto\":1500.00,\"codigoMoneda\":\"PEN\",\"obtenidoEn\":\"2026-10-10T18:00:00-05:00\"},\"detraccion\":{\"porcentaje\":0,\"monto\":0,\"cuentaBancaria\":\"\"}}" "201"
FACTURA_ID=$(python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" < .latest_response.json)
paso "Emitir factura" "POST" "$FACTURACION_URL/api/v1/facturas/$FACTURA_ID/emitir" "{\"serie\":\"F001\",\"correlativo\":1}" "200"

# 9. Cobranza: comprobar cuenta corriente
paso "Consultar cuenta corriente" "GET" "$COBRANZA_URL/api/v1/cuentas-corrientes/$CLIENTE_ID" "null" "200"
DEUDA_COUNT=$(python3 -c "import sys, json; data=json.load(sys.stdin); print(len(data.get('deudaPorMoneda', [])))" < .latest_response.json)
if [ "$DEUDA_COUNT" -eq 0 ]; then
    echo "Fallo: la cuenta corriente tiene deuda 0"
    exit 1
fi

echo "Flujo vertical completo: la orden llego hasta la cuenta por cobrar."
