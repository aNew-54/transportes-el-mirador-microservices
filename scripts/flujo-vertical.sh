#!/usr/bin/env bash
#
# Flujo vertical: una orden a credito que llega hasta la cuenta por cobrar, atravesando los siete
# contextos y once contratos de integracion.
#
# Levanta los siete el mismo, con el arranque compartido de lib/servicios.sh, y los para al salir.
# No hace falta nada mas que un MySQL de `docker compose up -d` y los jar de `./mvnw clean verify`.
#
# Se para en el primer paso que falle y escupe el cuerpo entero de la respuesta: es un problem+json
# y su `detail` dice exactamente que campo o que invariante lo rechazo. Seguir tras un fallo no
# tendria sentido, porque cada paso necesita el identificador del anterior.

set -uo pipefail

cd "$(dirname "$0")/.."
. "scripts/lib/servicios.sh"

RESPUESTAS=$(mktemp -d)

limpiar() {
    detener_servicios
    rm -rf "$RESPUESTAS"
}
trap limpiar EXIT INT TERM

arrancar_servicios || exit 1
echo ""

ULTIMA="$RESPUESTAS/ultima.json"

# Identificadores unicos por ejecucion. El script corre contra el MySQL de `docker compose`, que
# conserva lo de la pasada anterior: con literales fijos, la segunda ejecucion muere en el 409 del
# RUC repetido. Se generan aqui, no dentro de cada paso, porque varios pasos comparten el mismo.
SUFIJO=$(python3 -c "import random; print(f'{random.randrange(10**8):08d}')")
RUC="20${SUFIJO}9"
PLACA=$(python3 -c "import random,string; print(''.join(random.choices(string.ascii_uppercase,k=3))+'-'+f'{random.randrange(1000):03d}')")
LICENCIA_1="Q${SUFIJO}"
LICENCIA_2="R${SUFIJO}"
VIAJE_ID="VIA-${SUFIJO}"

paso() {
    descripcion="$1"; metodo="$2"; url="$3"; cuerpo="$4"; esperado="$5"

    if [ "$cuerpo" != "null" ]; then
        estado=$(curl -s -X "$metodo" "$url" -H "Content-Type: application/json" \
            -d "$cuerpo" -w "%{http_code}" -o "$ULTIMA")
    else
        estado=$(curl -s -X "$metodo" "$url" -w "%{http_code}" -o "$ULTIMA")
    fi

    if [ -z "$estado" ] || [ "$estado" = "000" ]; then
        echo "  FALLO  $descripcion: sin respuesta de $url" >&2
        exit 1
    fi

    if [ "$estado" != "$esperado" ]; then
        echo "" >&2
        echo "  FALLO  $descripcion" >&2
        echo "         $metodo $url" >&2
        echo "         recibido $estado, esperado $esperado" >&2
        echo "         $(cat "$ULTIMA")" >&2
        exit 1
    fi
    echo "  ok     $descripcion"
}

# El id del recurso que acaba de crear el paso anterior.
id_de() {
    python3 -c "import sys, json; print(json.load(sys.stdin)['$1'])" < "$ULTIMA"
}

# 1. Comercial: Registrar cliente
paso "Registrar cliente" "POST" "$COMERCIAL_URL/api/v1/clientes" "{\"ruc\":\"$RUC\",\"razonSocial\":\"Cliente Humo $SUFIJO\",\"modalidadDePago\":\"CREDITO\",\"plazoEnDias\":30}" "201"
CLIENTE_ID=$(id_de id)

# 2. Comercial: Registrar contrato marco
paso "Registrar contrato marco" "POST" "$COMERCIAL_URL/api/v1/contratos-marco" "{\"clienteId\":\"$CLIENTE_ID\",\"vigenteDesde\":\"2020-01-01\",\"vigenteHasta\":\"2030-12-31\",\"tiempoLibreHoras\":2,\"consolidacionPermitida\":true,\"consolidacionRestricciones\":[],\"tarifasPactadas\":[{\"rutaOrigen\":\"Lima\",\"rutaDestino\":\"Piura\",\"rutaCorredor\":\"Norte\",\"tipoUnidad\":\"FURGON\",\"precioMonto\":1500,\"precioMoneda\":\"PEN\"}]}" "201"
CONTRATO_ID=$(id_de id)

# 3. Unidades: Registrar unidad y SOAT (documento)
paso "Registrar unidad" "POST" "$UNIDADES_URL/api/v1/unidades" "{\"placa\":\"$PLACA\",\"tipo\":\"FURGON\",\"pesoMaximoKg\":15000,\"volumenMaximoM3\":30.0,\"kilometraje\":0,\"intervaloMantenimiento\":\"ACEITE_Y_FILTROS\"}" "201"
UNIDAD_ID=$(id_de id)
# Una unidad no es elegible con solo el SOAT: la evaluacion documental exige tambien revision
# tecnica, permiso MTC y habilitacion vehicular. Con uno solo, el contrato 2 responde elegible=false
# con tres DOCUMENTO_VENCIDO y el viaje no se puede asignar.
for TIPO_DOC in SOAT REVISION_TECNICA PERMISO_MTC HABILITACION_VEHICULAR; do
    paso "Documento $TIPO_DOC" "POST" "$UNIDADES_URL/api/v1/unidades/$UNIDAD_ID/documentos" \
        "{\"tipoDocumento\":\"$TIPO_DOC\",\"desde\":\"2020-01-01\",\"hasta\":\"2030-01-01\",\"numero\":\"$TIPO_DOC-$SUFIJO\"}" "201"
done

# 3. Conductores: Registrar conductores e inducciones (para cliente)
paso "Registrar conductor 1" "POST" "$CONDUCTORES_URL/api/v1/conductores" "{\"nombreCompleto\":\"Juan Perez\",\"numeroDeLicencia\":\"$LICENCIA_1\",\"categoriaDeLicencia\":\"A_IIIC\",\"licenciaDesde\":\"2020-01-01\",\"licenciaHasta\":\"2030-01-01\"}" "201"
COND1_ID=$(id_de id)
paso "Induccion cond1" "POST" "$CONDUCTORES_URL/api/v1/conductores/$COND1_ID/inducciones" "{\"clienteId\":\"$CLIENTE_ID\",\"vigenteDesde\":\"2020-01-01\",\"vigenteHasta\":\"2030-01-01\"}" "201"

paso "Registrar conductor 2" "POST" "$CONDUCTORES_URL/api/v1/conductores" "{\"nombreCompleto\":\"Pedro Perez\",\"numeroDeLicencia\":\"$LICENCIA_2\",\"categoriaDeLicencia\":\"A_IIIC\",\"licenciaDesde\":\"2020-01-01\",\"licenciaHasta\":\"2030-01-01\"}" "201"
COND2_ID=$(id_de id)
paso "Induccion cond2" "POST" "$CONDUCTORES_URL/api/v1/conductores/$COND2_ID/inducciones" "{\"clienteId\":\"$CLIENTE_ID\",\"vigenteDesde\":\"2020-01-01\",\"vigenteHasta\":\"2030-01-01\"}" "201"

# 4. Comercial: Crear orden de servicio a CREDITO
paso "Crear orden" "POST" "$COMERCIAL_URL/api/v1/ordenes" "{\"clienteId\":\"$CLIENTE_ID\",\"contratoId\":\"$CONTRATO_ID\",\"tipoUnidad\":\"FURGON\",\"cargaPesoKg\":10000,\"cargaVolumenM3\":25.0,\"cargaTipo\":\"GENERAL\",\"rutaOrigen\":\"Lima\",\"rutaDestino\":\"Piura\",\"rutaCorredor\":\"Norte\",\"cargaEmbalaje\":\"CAJAS\",\"cargaNaturaleza\":\"SECA\",\"rutaDistanciaKm\":1000,\"ventanaInicio\":\"2026-10-10T08:00:00-05:00\",\"ventanaFin\":\"2026-10-10T18:00:00-05:00\",\"modalidadDePago\":\"CREDITO\",\"plazoEnDias\":30}" "201"
ORDEN_ID=$(id_de id)

# 5. Comercial: Confirmar orden
paso "Confirmar orden" "POST" "$COMERCIAL_URL/api/v1/ordenes/$ORDEN_ID/confirmar" "null" "200"

# 6. Programacion: Crear viaje, consolidar y asignar recursos, programar y despachar
# NOTA: En la API de Programación, el contrato 1 se ejerce internamente dentro del servicio en dos lugares:
# 1) `consolidarOrden` (POST /{id}/ordenes), que es para consolidar una carga *adicional*.
# 2) `asignarRecursos` (POST /{id}/recursos), que verifica los requisitos de la unidad y del cliente contra las órdenes.
# Como el flujo sólo define una orden de carga en el paso 4, la agregamos directo en "planificar" y 
# usamos "asignarRecursos" (que llama al contrato 1 internamente para verificar al cliente y unidad requerida),
# evitando consolidar la misma orden por segunda vez, lo que duplicaría el volumen.
paso "Planificar viaje" "POST" "$PROGRAMACION_URL/api/v1/viajes" "{\"id\":\"$VIAJE_ID\",\"ruta\":{\"origen\":\"Lima\",\"destino\":\"Piura\",\"corredor\":\"Norte\"},\"ventana\":{\"desde\":\"2026-10-10T08:00:00-05:00\",\"hasta\":\"2026-10-10T18:00:00-05:00\"},\"cargaInicial\":{\"ordenDeServicioId\":\"$ORDEN_ID\",\"pesoKg\":10000,\"volumenM3\":25.0,\"tipo\":\"GENERAL\",\"secuenciaDeDescarga\":1}}" "201"

paso "Asignar recursos" "POST" "$PROGRAMACION_URL/api/v1/viajes/$VIAJE_ID/recursos" "{\"unidadId\":\"$UNIDAD_ID\",\"conductorIds\":[\"$COND1_ID\",\"$COND2_ID\"],\"conRelevo\":true}" "200"

paso "Programar viaje" "POST" "$PROGRAMACION_URL/api/v1/viajes/$VIAJE_ID/programar" "{\"hojaDeRuta\":[{\"secuencia\":1,\"tipo\":\"CARGA\",\"ordenDeServicioId\":\"$ORDEN_ID\",\"ubicacion\":{\"direccion\":\"Lima 123\"},\"horaEstimada\":\"2026-10-10T09:00:00-05:00\"},{\"secuencia\":2,\"tipo\":\"DESCARGA\",\"ordenDeServicioId\":\"$ORDEN_ID\",\"ubicacion\":{\"direccion\":\"Piura 456\"},\"horaEstimada\":\"2026-10-10T17:00:00-05:00\"}],\"observaciones\":\"Cuidado\"}" "200"

paso "Despachar viaje" "POST" "$PROGRAMACION_URL/api/v1/viajes/$VIAJE_ID/despachar" "null" "200"

# 7. Ejecucion: check-list, iniciar, conformidades, cerrar
paso "Crear ejecucion" "POST" "$EJECUCION_URL/api/v1/ejecuciones" "{\"viajeId\":\"$VIAJE_ID\"}" "201"
paso "Registrar checklist" "POST" "$EJECUCION_URL/api/v1/ejecuciones/$VIAJE_ID/checklist" "{\"aprobado\":true,\"observaciones\":[]}" "200"
paso "Iniciar ejecucion" "POST" "$EJECUCION_URL/api/v1/ejecuciones/$VIAJE_ID/iniciar" "null" "200"
paso "Conformidad parada 1" "POST" "$EJECUCION_URL/api/v1/ejecuciones/$VIAJE_ID/paradas/1/conformidad" "{\"estado\":\"FIRMADA\",\"recibidoPor\":\"Encargado 1\"}" "201"
paso "Conformidad parada 2" "POST" "$EJECUCION_URL/api/v1/ejecuciones/$VIAJE_ID/paradas/2/conformidad" "{\"estado\":\"FIRMADA\",\"recibidoPor\":\"Encargado 2\"}" "201"
# El contrato 8 no crea la factura: la desbloquea. FAC-01 mantiene BLOQUEADA una factura sin
# conformidad, asi que la factura tiene que existir ANTES de cerrar el viaje. Ponerla despues
# —como estaba— hacia que Facturacion respondiera 404 al contrato 8 y el cierre saliera 503.
paso "Abrir factura" "POST" "$FACTURACION_URL/api/v1/facturas" "{\"ordenDeServicioId\":\"$ORDEN_ID\",\"clienteId\":\"$CLIENTE_ID\",\"snapshot\":{\"tarifaMonto\":1500.00,\"codigoMoneda\":\"PEN\",\"obtenidoEn\":\"2026-10-10T18:00:00-05:00\"},\"detraccion\":{\"porcentaje\":0,\"monto\":0,\"cuentaBancaria\":\"\"}}" "201"
FACTURA_ID=$(id_de id)

# EJV-03: la ejecucion solo se cierra desde ENTREGADA, y solo se entrega con todas las paradas
# firmadas. Este endpoint no existia: el agregado tenia marcarEntregada con su invariante y sus
# pruebas, y ningun controlador lo exponia.
paso "Marcar entregada" "POST" "$EJECUCION_URL/api/v1/ejecuciones/$VIAJE_ID/entregar" "null" "200"

paso "Cerrar ejecucion" "POST" "$EJECUCION_URL/api/v1/ejecuciones/$VIAJE_ID/cerrar" "{\"kilometrajeFinal\":1000,\"horasPorConductor\":[{\"conductorId\":\"$COND1_ID\",\"horas\":4,\"desde\":\"2026-10-10T09:00:00-05:00\",\"hasta\":\"2026-10-10T13:00:00-05:00\"},{\"conductorId\":\"$COND2_ID\",\"horas\":4,\"desde\":\"2026-10-10T13:00:00-05:00\",\"hasta\":\"2026-10-10T17:00:00-05:00\"}],\"conceptosFacturables\":[{\"ordenDeServicioId\":\"$ORDEN_ID\",\"concepto\":\"ESTIBA\",\"monto\":\"1500.00\",\"moneda\":\"PEN\"}]}" "200"

paso "Emitir factura" "POST" "$FACTURACION_URL/api/v1/facturas/$FACTURA_ID/emitir" "{\"serie\":\"F001\",\"correlativo\":1}" "200"

# 9. Cobranza: comprobar cuenta corriente
paso "Consultar cuenta corriente" "GET" "$COBRANZA_URL/api/v1/cuentas-corrientes/$CLIENTE_ID" "null" "200"
DEUDA_COUNT=$(python3 -c "import sys, json; print(len(json.load(sys.stdin).get('deudaPorMoneda', [])))" < "$ULTIMA")
if [ "$DEUDA_COUNT" -eq 0 ]; then
    echo "" >&2
    echo "  FALLO  La cuenta corriente no tiene deuda: la factura no llego al ledger" >&2
    exit 1
fi

echo ""
echo "Flujo vertical completo: la orden llego hasta la cuenta por cobrar."
