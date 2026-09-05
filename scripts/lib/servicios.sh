#!/bin/sh
#
# Arranque y parada de los siete microservicios. Se sourcea; no se ejecuta.
#
# Lo usan smoke-test.sh y flujo-vertical.sh. Vive aparte porque el segundo necesita exactamente el
# mismo arranque —los mismos puertos desplazados y las mismas siete variables de entorno— y una
# copia se habria desincronizado a la primera: la leccion ya la dio S5, cuando smoke-test.sh daba
# verde con siete servicios que no se encontraban entre si.

SERVICES="comercial:8010 programacion:8020 ejecucion:8030 unidades:8040 conductores:8050 facturacion:8060 cobranza:8070"
PORT_OFFSET="${SMOKE_PORT_OFFSET:-10000}"
PIDS=""
LOG_DIR="target/smoke-logs"

# El puerto real de un contexto, ya desplazado. Lo usan los curl del flujo vertical.
puerto_de() {
    for especificacion in $SERVICES; do
        if [ "${especificacion%%:*}" = "$1" ]; then
            echo $(( ${especificacion##*:} + PORT_OFFSET ))
            return 0
        fi
    done
    echo "Contexto desconocido: $1" >&2
    return 1
}

detener_servicios() {
    for pid in $PIDS; do
        kill "$pid" 2>/dev/null || true
    done
    for pid in $PIDS; do
        wait "$pid" 2>/dev/null || true
    done
}

# Cada consumidor lee `clients.<x>.url`, que por defecto apunta al puerto SIN desplazar. Sin estas
# siete variables, un servicio en el 18010 buscaria a sus vecinos en el 8020, donde no hay nadie.
exportar_urls() {
    COMERCIAL_URL="http://localhost:$(puerto_de comercial)";     export COMERCIAL_URL
    PROGRAMACION_URL="http://localhost:$(puerto_de programacion)"; export PROGRAMACION_URL
    EJECUCION_URL="http://localhost:$(puerto_de ejecucion)";      export EJECUCION_URL
    UNIDADES_URL="http://localhost:$(puerto_de unidades)";        export UNIDADES_URL
    CONDUCTORES_URL="http://localhost:$(puerto_de conductores)";  export CONDUCTORES_URL
    FACTURACION_URL="http://localhost:$(puerto_de facturacion)";  export FACTURACION_URL
    COBRANZA_URL="http://localhost:$(puerto_de cobranza)";        export COBRANZA_URL
}

arrancar_servicios() {
    mkdir -p "$LOG_DIR"
    exportar_urls

    for especificacion in $SERVICES; do
        servicio=${especificacion%%:*}
        puerto=$(( ${especificacion##*:} + PORT_OFFSET ))
        jar="msvc-${servicio}/target/msvc-${servicio}-0.1.0-SNAPSHOT.jar"

        if [ ! -f "$jar" ]; then
            echo "No se encontro $jar. Ejecuta ./mvnw clean verify primero." >&2
            return 1
        fi

        java -jar "$jar" --server.port="$puerto" >"${LOG_DIR}/msvc-${servicio}.log" 2>&1 &
        PIDS="$PIDS $!"
    done

    for especificacion in $SERVICES; do
        servicio=${especificacion%%:*}
        puerto=$(( ${especificacion##*:} + PORT_OFFSET ))
        intentos=0

        until curl --fail --silent "http://localhost:${puerto}/actuator/health" \
            | grep --quiet '"status":"UP"'; do
            intentos=$((intentos + 1))
            if [ "$intentos" -ge 60 ]; then
                echo "msvc-${servicio} no alcanzo el estado UP en el puerto ${puerto}." >&2
                tail -n 80 "${LOG_DIR}/msvc-${servicio}.log" >&2
                return 1
            fi
            sleep 1
        done

        echo "msvc-${servicio}: UP"
    done
}
