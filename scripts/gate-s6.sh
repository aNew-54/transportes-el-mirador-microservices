#!/usr/bin/env bash
#
# Gate del slice S6-cierre. Uso:  ./scripts/gate-s6.sh [ruta-raiz]
#
# Un gate de un solo modulo: S6 existe solo en msvc-ejecucion. Comprueba lo que la spec
# (docs/delivery/servicios/msvc-ejecucion.md, seccion S6-cierre) exige y una revision a ojo se salta.
#
# Misma nota que gate-s3.sh y gate-s5.sh sobre `grep -q`: con `pipefail` cierra la tuberia al primer
# acierto y el grep de la izquierda muere con 141, que el `if` lee como «sin coincidencias». Cada
# comprobacion captura su salida en una variable y mira si esta vacia.

set -uo pipefail

RAIZ="${1:-.}"
MOD="$RAIZ/msvc-ejecucion"
SRC="$MOD/src/main/java/pe/edu/unc/elmirador/ejecucion"
TST="$MOD/src/test/java/pe/edu/unc/elmirador/ejecucion"
FALLOS=0

fallo() { printf '  FALLO  %s\n' "$1"; FALLOS=$((FALLOS + 1)); }
ok()    { printf '  ok     %s\n' "$1"; }
detalle() { printf '%s\n' "$1" | sed 's/^/         /'; }
comprobar() { if [ -n "$2" ]; then fallo "$1"; detalle "$2"; else ok "$1"; fi; }

SERVICIO="$SRC/services/EjecucionDeViajeService.java"
AGREGADO="$SRC/models/entity/EjecucionDeViaje.java"
PETICION="$SRC/dto/request/CerrarEjecucionRequest.java"

printf 'S6-cierre · msvc-ejecucion\n'

# ---------------------------------------------------------------------------------------------
# 1. El defecto que el slice existe para cerrar: LIQ-04 no puede volver al cuerpo de la peticion.
# ---------------------------------------------------------------------------------------------
comprobar "CerrarEjecucionRequest no acepta un veredicto sobre las liquidaciones" \
    "$(grep -niE 'liquidacion' "$PETICION" | grep -viE '^\s*[0-9]+: *\*|^\s*[0-9]+: *//|LiquidacionDeViajeRepository' || true)"

comprobar "El servicio cuenta las liquidaciones el mismo" \
    "$([ -z "$(grep -n 'findByViajeIdAndEstadoNot' "$SERVICIO" || true)" ] \
        && echo "$SERVICIO no llama a findByViajeIdAndEstadoNot: LIQ-04 se estaria comprobando contra otra cosa" || true)"

# ---------------------------------------------------------------------------------------------
# 2. Los cinco empujes existen y van DESPUES de que el agregado acepte cerrar.
# ---------------------------------------------------------------------------------------------
for LLAMADA in \
    'unidadesGateway.reportarKilometraje' \
    'unidadesGateway.reportarFalla' \
    'conductoresGateway.reportarHoras' \
    'comercialGateway.reportarEspera' \
    'facturacionGateway.registrarConformidad'
do
    # Se descartan las lineas comentadas: con un grep a secas, comentar la llamada dejaba el gate
    # en verde. Se comprobo inyectandolo, y en su primera pasada este gate se lo trago.
    comprobar "El servicio llama a $LLAMADA" \
        "$([ -z "$(grep -nE "^[^/*]*$LLAMADA" "$SERVICIO" || true)" ] \
            && echo "el cliente de $LLAMADA sigue escrito y sin llamar" || true)"
done

LINEA_CERRAR=$(grep -n 'ejecucion\.cerrar(' "$SERVICIO" | head -1 | cut -d: -f1)
LINEA_PRIMER_EMPUJE=$(grep -n 'reportarAUnidades(ejecucion)' "$SERVICIO" | head -1 | cut -d: -f1)
comprobar "Los empujes van despues de ejecucion.cerrar(...)" \
    "$( { [ -z "$LINEA_CERRAR" ] || [ -z "$LINEA_PRIMER_EMPUJE" ] || [ "$LINEA_PRIMER_EMPUJE" -lt "$LINEA_CERRAR" ]; } \
        && echo "un viaje que no se puede cerrar estaria emitiendo igualmente su kilometraje" || true)"

comprobar "cerrar es transaccional: un empuje fallido no deja el viaje cerrado" \
    "$([ -z "$(grep -n -B2 'public EjecucionDeViajeResponse cerrar' "$SERVICIO" | grep '@Transactional' || true)" ] \
        && echo "cerrar sin @Transactional: el estado CERRADA sobreviviria a un 503 de Facturacion" || true)"

# ---------------------------------------------------------------------------------------------
# 3. Lo que el agregado debe poner, y el cuerpo no.
# ---------------------------------------------------------------------------------------------
comprobar "incidenciasSinResolver lo pone el agregado" \
    "$([ -z "$(grep -n 'ejecucion.incidenciasSinResolver()' "$SERVICIO" || true)" ] \
        && echo "si viniera del cuerpo, mandar la lista vacia desbloquearia FAC-05 desde fuera" || true)"

comprobar "crear no tira los conductores de la hoja de ruta" \
    "$([ -z "$(grep -n 'hoja.conductorIds()' "$SERVICIO" || true)" ] \
        && echo "sin conductorIds el contrato 6 no tiene a quien reportarle horas" || true)"

comprobar "El agregado valida la cobertura de horas" \
    "$([ -z "$(grep -n 'validarCoberturaDeHoras' "$AGREGADO" || true)" ] \
        && echo "la regla de que las horas cubran a los conductores asignados no esta en el agregado" || true)"

# Regla de CLAUDE.md: un if de negocio dentro de un service es un defecto.
comprobar "El servicio no decide que incidencia es de quien" \
    "$(grep -nE 'TipoDeIncidencia\.(AVERIA|DANIO|FALTANTE|CLIMA|DEMORA|BLOQUEO_DE_VIA|RECHAZO_DE_CARGA)' "$SERVICIO" || true)"

# ---------------------------------------------------------------------------------------------
# 4. Los dos 409 remotos que no son un 503.
# ---------------------------------------------------------------------------------------------
for PAR in "UnidadesGateway:UNI-03" "ConductoresGateway:CON-02"; do
    PASARELA="$SRC/clients/${PAR%%:*}.java"
    INV="${PAR##*:}"
    # Se busca el catch, no el nombre: con `grep FeignException.Conflict` a secas el import solo ya
    # daria verde. Misma leccion que el catch de RetryableException en gate-s5.sh.
    comprobar "${PAR%%:*} atrapa FeignException.Conflict" \
        "$([ -z "$(grep -nE 'catch *\(.*FeignException\.Conflict' "$PASARELA" || true)" ] \
            && echo "$PASARELA manda el 409 de $INV a un 503: diria «no pude comprobarlo» cuando el otro servicio si respondio" || true)"

    LINEA_CONFLICT=$(grep -nE 'catch *\(.*FeignException\.Conflict' "$PASARELA" | head -1 | cut -d: -f1)
    LINEA_GENERICO=$(grep -nE 'catch *\(FeignException ' "$PASARELA" | head -1 | cut -d: -f1)
    comprobar "${PAR%%:*} lo atrapa antes que el FeignException generico" \
        "$( { [ -z "$LINEA_CONFLICT" ] || [ -z "$LINEA_GENERICO" ] || [ "$LINEA_CONFLICT" -gt "$LINEA_GENERICO" ]; } \
            && echo "el catch generico va primero: el 409 nunca llegaria al especifico" || true)"

    comprobar "${PAR%%:*} sigue atrapando RetryableException" \
        "$([ -z "$(grep -nE 'catch *\(.*RetryableException' "$PASARELA" || true)" ] \
            && echo "$PASARELA dejo de traducir el socket caido" || true)"
done

# ---------------------------------------------------------------------------------------------
# 5. La traduccion de vocabulario del contrato 8.
# ---------------------------------------------------------------------------------------------
VO_CONF="$SRC/models/vo/EstadoConformidad.java"
comprobar "EstadoConformidad traduce al idioma del contrato 8" \
    "$([ -z "$(grep -n 'codigoDelContrato' "$VO_CONF" || true)" ] \
        && echo "sin traduccion, PENDIENTE u OBSERVADA viajarian tal cual a Facturacion" || true)"

comprobar "PENDIENTE lanza en vez de inventarse un estado" \
    "$([ -z "$(grep -nE 'case PENDIENTE -> throw' "$VO_CONF" || true)" ] \
        && echo "un RECHAZADA inventado enterraria en Facturacion un defecto que esta antes" || true)"

# ---------------------------------------------------------------------------------------------
# 6. Esquema. Regla 11: lo crea Flyway, y con ddl-auto=validate una columna sin migracion rompe.
# ---------------------------------------------------------------------------------------------
MIG="$MOD/src/main/resources/db/migration/V2__cierre_de_ejecucion.sql"
comprobar "Existe la migracion V2" \
    "$([ ! -f "$MIG" ] && echo "falta $MIG: con ddl-auto=validate el modulo no arranca" || true)"

comprobar "kilometraje_final admite NULL" \
    "$([ -f "$MIG" ] && [ -z "$(grep -n 'kilometraje_final INT NULL' "$MIG" || true)" ] \
        && echo "no hay valor por defecto honesto para un kilometraje que aun no se ha leido del tablero" || true)"

comprobar "Existe la tabla de conductores" \
    "$([ -f "$MIG" ] && [ -z "$(grep -n 'CREATE TABLE ejecucion_conductores' "$MIG" || true)" ] \
        && echo "el @ElementCollection del agregado no tiene tabla" || true)"

# ---------------------------------------------------------------------------------------------
# 7. Pruebas que la spec exige, y el aislamiento de siempre.
# ---------------------------------------------------------------------------------------------
comprobar "Hay prueba de que LIQ-04 ya no es falseable desde el cuerpo" \
    "$([ -z "$(grep -rn 'CerrarEjecucionRequest.class' "$TST" || true)" ] \
        && echo "sin una prueba por reflexion, el campo puede volver al DTO sin que nada se ponga rojo" || true)"

comprobar "Hay prueba del 409 como conflicto de dominio" \
    "$([ -z "$(grep -rn 'ConflictoDeRecursoException' "$TST/clients" || true)" ] \
        && echo "nadie comprueba que el 409 remoto no sale como 503" || true)"

comprobar "Aislamiento entre contextos" \
    "$(grep -rn "^import pe\.edu\.unc\.elmirador\." "$MOD/src" 2>/dev/null | grep -v "elmirador\.ejecucion\." || true)"

printf '\n'
if [ "$FALLOS" -eq 0 ]; then
    printf 'S6-cierre: %s\n' "sin fallos"
else
    printf 'S6-cierre: %s fallo(s)\n' "$FALLOS"
fi
exit "$FALLOS"
