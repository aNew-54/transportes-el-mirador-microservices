#!/usr/bin/env bash
#
# Gate del slice S5-clientes. Uso:  ./scripts/gate-s5.sh <contexto> [ruta-raiz]
#
# Comprueba lo que la receta (docs/delivery/README.md §10) exige y una revision a ojo se salta.
# No sustituye a `./mvnw -pl msvc-<ctx> test`: lo complementa.
#
# Misma nota que gate-s3.sh sobre `grep -q`: con `pipefail` cierra la tuberia al primer acierto y el
# grep de la izquierda muere con 141, que el `if` lee como «sin coincidencias». Aqui cada comprobacion
# captura su salida en una variable y mira si esta vacia.

set -uo pipefail

CTX="${1:?uso: gate-s5.sh <contexto> [ruta-raiz]}"
RAIZ="${2:-.}"
MOD="$RAIZ/msvc-$CTX"
SRC="$MOD/src/main/java/pe/edu/unc/elmirador/$CTX"
TST="$MOD/src/test/java/pe/edu/unc/elmirador/$CTX"
FALLOS=0

fallo() { printf '  FALLO  %s\n' "$1"; FALLOS=$((FALLOS + 1)); }
ok()    { printf '  ok     %s\n' "$1"; }
detalle() { printf '%s\n' "$1" | sed 's/^/         /'; }
comprobar() { if [ -n "$2" ]; then fallo "$1"; detalle "$2"; else ok "$1"; fi; }

# Regla 10 de CLAUDE.md: solo consume quien tiene flecha saliente en el mapa de contexto.
case "$CTX" in
    comercial)    PROVEEDORES="Cobranza" ;;
    facturacion)  PROVEEDORES="Comercial Cobranza" ;;
    programacion) PROVEEDORES="Comercial Unidades Conductores" ;;
    ejecucion)    PROVEEDORES="Programacion Unidades Conductores Comercial Facturacion" ;;
    unidades|conductores|cobranza) PROVEEDORES="" ;;
    *) echo "contexto desconocido: $CTX" >&2; exit 2 ;;
esac

echo "== gate S5 · msvc-$CTX"

# ---------------------------------------------------------------- proveedores puros
if [ -z "$PROVEEDORES" ]; then
    echo "  (proveedor puro: sin flecha saliente en el mapa de contexto)"

    comprobar "sin la dependencia openfeign" \
        "$(grep -n "openfeign" "$MOD/pom.xml" 2>/dev/null || true)"

    comprobar "sin @EnableFeignClients" \
        "$(grep -rn "EnableFeignClients" "$SRC" 2>/dev/null || true)"

    if [ -d "$SRC/clients" ]; then
        fallo "tiene el paquete clients/ y no consume ningun contrato"
        detalle "$(ls "$SRC/clients")"
    else
        ok "sin el paquete clients/"
    fi

    echo
    [ "$FALLOS" -eq 0 ] && echo "== msvc-$CTX: gate S5 limpio" \
        || echo "== msvc-$CTX: $FALLOS fallo(s) en el gate S5"
    exit $((FALLOS > 0))
fi

# ---------------------------------------------------------------- consumidores
comprobar "declara @EnableFeignClients" \
    "$([ -z "$(grep -rn "EnableFeignClients" "$SRC" 2>/dev/null || true)" ] \
        && echo "no aparece en ningun archivo de $SRC" || true)"

for P in $PROVEEDORES; do
    CLIENTE="$SRC/clients/${P}Client.java"
    PASARELA="$SRC/clients/${P}Gateway.java"
    EXCEPCION="$SRC/exceptions/${P}IntegrationException.java"

    [ -f "$CLIENTE" ]   && ok "clients/${P}Client"   || fallo "falta clients/${P}Client.java"
    [ -f "$PASARELA" ]  && ok "clients/${P}Gateway"  || fallo "falta clients/${P}Gateway.java"

    # Regla 5 de contracts.md: una excepcion de integracion propia por contexto consumido.
    if [ ! -f "$EXCEPCION" ]; then
        fallo "falta exceptions/${P}IntegrationException.java"
    elif [ -n "$(grep -n "extends RuntimeException" "$EXCEPCION" || true)" ]; then
        ok "${P}IntegrationException extiende RuntimeException"
    else
        # Si heredara de la excepcion de dominio, el @ExceptionHandler generico se la comeria
        # y el fallo remoto saldria como 422 en vez de 503.
        fallo "${P}IntegrationException NO extiende RuntimeException directamente"
        detalle "$(grep -n "extends" "$EXCEPCION" || true)"
    fi

    # RetryableException NO hereda de FeignException: es la del socket que no abre y la del
    # read-timeout, o sea el unico caso que este slice existe para cubrir.
    if [ -f "$PASARELA" ]; then
        # Se busca el catch, no el nombre: con `grep RetryableException` a secas el import solo ya
        # daba verde, y este gate se lo trago en su primera prueba en rojo con el catch quitado.
        comprobar "${P}Gateway atrapa RetryableException" \
            "$([ -z "$(grep -nE "catch *\(.*RetryableException" "$PASARELA" || true)" ] \
                && echo "$PASARELA solo atrapa FeignException: el proveedor caido se escapa sin traducir" || true)"
    fi

    # La prueba del cliente real contra un stub es lo unico que comprueba la forma pactada.
    comprobar "prueba de stub de ${P}" \
        "$([ ! -f "$TST/clients/${P}ClientStubTest.java" ] \
            && echo "falta $TST/clients/${P}ClientStubTest.java" || true)"
done

# El servicio de aplicacion inyecta el gateway, nunca el @FeignClient.
FUGAS=$(grep -rn "Client " "$SRC/services" 2>/dev/null \
    | grep -E "(private final|,|\() *[A-Za-z]+Client " || true)
comprobar "ningun service inyecta el @FeignClient directamente" "$FUGAS"

# La url sale de una propiedad, nunca de un host literal.
LITERALES=$(grep -rn "url *= *\"http" "$SRC/clients" 2>/dev/null || true)
comprobar "ninguna url literal en un @FeignClient" "$LITERALES"

# Regla 4 de contracts.md.
for T in connect-timeout read-timeout; do
    comprobar "application.properties declara $T" \
        "$([ -z "$(grep -n "openfeign.client.config.default.$T" \
            "$MOD/src/main/resources/application.properties" 2>/dev/null || true)" ] \
            && echo "no aparece" || true)"
done

# Regla 5: el fallo remoto no se convierte en «no existe».
DEGRADA=$(grep -rn "Optional.empty()\|return null" "$SRC/clients" 2>/dev/null || true)
comprobar "ningun gateway degrada el fallo a un vacio" "$DEGRADA"

# El 503 del contrato, en el unico sitio que conoce codigos HTTP.
MANEJADOR="$SRC/controllers/ManejadorDeErrores.java"
comprobar "ManejadorDeErrores devuelve 503 para el fallo de integracion" \
    "$([ -z "$(grep -n "SERVICE_UNAVAILABLE" "$MANEJADOR" 2>/dev/null || true)" ] \
        && echo "$MANEJADOR no menciona SERVICE_UNAVAILABLE" || true)"

# Regla 1 del contrato.
CRUCES=$(grep -rn "^import pe\.edu\.unc\.elmirador\." "$MOD/src" 2>/dev/null \
    | grep -v "elmirador\.$CTX\." || true)
comprobar "aislado de los otros contextos" "$CRUCES"

echo
[ "$FALLOS" -eq 0 ] && echo "== msvc-$CTX: gate S5 limpio" \
    || echo "== msvc-$CTX: $FALLOS fallo(s) en el gate S5"
exit $((FALLOS > 0))
