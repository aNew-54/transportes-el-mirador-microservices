#!/bin/sh
set -eu

SERVICES="comercial:8010 programacion:8020 ejecucion:8030 unidades:8040 conductores:8050 facturacion:8060 cobranza:8070"
PORT_OFFSET="${SMOKE_PORT_OFFSET:-10000}"
PIDS=""
LOG_DIR="target/smoke-logs"

cleanup() {
    for pid in $PIDS; do
        kill "$pid" 2>/dev/null || true
    done
    for pid in $PIDS; do
        wait "$pid" 2>/dev/null || true
    done
}

trap cleanup EXIT INT TERM
mkdir -p "$LOG_DIR"

# Los siete arrancan desplazados por PORT_OFFSET para no chocar con nada que este escuchando.
# Desde S5 eso ya no basta: cada consumidor lee `clients.<x>.url`, que por defecto apunta al puerto
# SIN desplazar. Sin exportar estas variables, un servicio en el 18010 buscaria a sus vecinos en el
# 8020, donde no hay nadie, y este script seguiria dando verde porque solo mira /actuator/health.
# Siete servicios sanos que no se encuentran entre si no son un sistema.
export COMERCIAL_URL="http://localhost:$((8010 + PORT_OFFSET))"
export PROGRAMACION_URL="http://localhost:$((8020 + PORT_OFFSET))"
export EJECUCION_URL="http://localhost:$((8030 + PORT_OFFSET))"
export UNIDADES_URL="http://localhost:$((8040 + PORT_OFFSET))"
export CONDUCTORES_URL="http://localhost:$((8050 + PORT_OFFSET))"
export FACTURACION_URL="http://localhost:$((8060 + PORT_OFFSET))"
export COBRANZA_URL="http://localhost:$((8070 + PORT_OFFSET))"

for specification in $SERVICES; do
    service=${specification%%:*}
    default_port=${specification##*:}
    port=$((default_port + PORT_OFFSET))
    jar="msvc-${service}/target/msvc-${service}-0.1.0-SNAPSHOT.jar"
    log="${LOG_DIR}/msvc-${service}.log"

    if [ ! -f "$jar" ]; then
        echo "No se encontró $jar. Ejecuta ./mvnw clean verify primero." >&2
        exit 1
    fi

    java -jar "$jar" --server.port="$port" >"$log" 2>&1 &
    PIDS="$PIDS $!"
done

for specification in $SERVICES; do
    service=${specification%%:*}
    default_port=${specification##*:}
    port=$((default_port + PORT_OFFSET))
    attempts=0

    until curl --fail --silent "http://localhost:${port}/actuator/health" \
        | grep --quiet '"status":"UP"'; do
        attempts=$((attempts + 1))
        if [ "$attempts" -ge 60 ]; then
            echo "msvc-${service} no alcanzó el estado UP en el puerto ${port}." >&2
            tail -n 80 "${LOG_DIR}/msvc-${service}.log" >&2
            exit 1
        fi
        sleep 1
    done

    echo "msvc-${service}: UP"
done

echo "Los siete microservicios iniciaron correctamente."

# Queda pendiente la comprobacion de cableado de extremo a extremo: una llamada real de un consumidor
# a un proveedor. No se pone aqui una a medias. El primer intento enviaba una orden al contado, y una
# orden al contado NO consulta a Cobranza —esa es justo la segunda mitad de CLI-01—, asi que habria
# dado verde sin que saliera una sola peticion entre servicios. Y una a credito con un cliente
# inexistente muere en el 404 de Comercial antes de llegar a Cobranza.
#
# Para que la comprobacion signifique algo hacen falta datos sembrados en dos contextos a la vez: un
# cliente y un contrato marco en Comercial, y una cuenta corriente para ese mismo cliente en Cobranza.
# Eso es el hito de flujo vertical del backlog, no una linea de curl al final de este script.
#
# Mientras tanto, lo que este script garantiza es lo que dice: los siete arrancan, y desde ahora
# arrancan sabiendo donde estan los demas.
