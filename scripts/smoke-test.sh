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
