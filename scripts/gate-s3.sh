#!/usr/bin/env bash
#
# Gate del slice S3-api-publica. Uso:  ./scripts/gate-s3.sh <contexto> [ruta-raiz]
#
# Comprueba lo que la receta (docs/delivery/README.md §8) exige y una revision a ojo se salta.
# No sustituye a `./mvnw -pl msvc-<ctx> test`: lo complementa.
#
# Nota sobre la forma de los chequeos: ninguno usa `... | grep -q`. Con `pipefail`, el `-q` cierra
# la tuberia al primer acierto, el grep de la izquierda muere con SIGPIPE y devuelve 141, y el `if`
# lee ese 141 como «no hay coincidencias». El gate daba verde con la violacion delante. Cada
# comprobacion captura su salida en una variable y mira si esta vacia, que es determinista.

set -uo pipefail

CTX="${1:?uso: gate-s3.sh <contexto> [ruta-raiz]}"
RAIZ="${2:-.}"
MOD="$RAIZ/msvc-$CTX"
SRC="$MOD/src/main/java/pe/edu/unc/elmirador/$CTX"
FALLOS=0

fallo() { printf '  FALLO  %s\n' "$1"; FALLOS=$((FALLOS + 1)); }
ok()    { printf '  ok     %s\n' "$1"; }

detalle() { printf '%s\n' "$1" | sed 's/^/         /'; }

# Quita las lineas que son comentario: las que empiezan por *, // o /*.
solo_codigo() { grep -v ":[0-9]*: *\*" | grep -v ":[0-9]*: *//" | grep -v ":[0-9]*: */\*"; }

# Comprueba: <descripcion> <salida-del-grep>. Vacio = ok.
comprobar() {
    if [ -n "$2" ]; then fallo "$1"; detalle "$2"; else ok "$1"; fi
}

echo "== gate S3 · msvc-$CTX"

[ -f "$SRC/config/RelojConfig.java" ] \
    && ok "config/RelojConfig" || fallo "falta config/RelojConfig.java"

[ -f "$SRC/controllers/ManejadorDeErrores.java" ] \
    && ok "controllers/ManejadorDeErrores" || fallo "falta controllers/ManejadorDeErrores.java"

for e in RecursoNoEncontradoException ConflictoDeRecursoException; do
    if [ ! -f "$SRC/exceptions/$e.java" ]; then
        fallo "falta exceptions/$e.java"
    elif grep -q "extends RuntimeException" "$SRC/exceptions/$e.java"; then
        ok "$e extiende RuntimeException"
    else
        fallo "$e NO extiende RuntimeException: caeria en el 422 por defecto"
    fi
done

# Regla 1 del contrato: ningun modulo importa otro contexto.
CRUCES=$(grep -rn "^import pe\.edu\.unc\.elmirador\." "$MOD/src" 2>/dev/null \
    | grep -v "elmirador\.$CTX\." || true)
comprobar "aislado de los otros contextos" "$CRUCES"

# Regla D1: el reloj se inyecta.
RELOJ=$(grep -rn "LocalDate\.now()\|Instant\.now()\|OffsetDateTime\.now()\|LocalDateTime\.now()" \
    "$MOD/src/main" 2>/dev/null | solo_codigo || true)
comprobar "el reloj siempre llega inyectado" "$RELOJ"

# Regla 2: ninguna entidad JPA cruza la frontera HTTP.
FUGA=$(grep -rln "models\.entity" "$SRC/dto" "$SRC/controllers" 2>/dev/null || true)
comprobar "la entidad JPA no cruza la frontera HTTP" "$FUGA"

# El controlador no atrapa nada: la traduccion vive en un solo sitio.
ATRAPA=$(grep -rn "catch *(" "$SRC/controllers" 2>/dev/null | grep -v ManejadorDeErrores || true)
comprobar "los controladores no atrapan nada" "$ATRAPA"

# Imports de Boot 3 / Jackson 2, que en Boot 4 no existen.
VIEJOS=$(grep -rn "boot\.test\.autoconfigure\.web\.servlet\|com\.fasterxml\.jackson" \
    "$MOD/src" 2>/dev/null || true)
comprobar "imports de Boot 4 y Jackson 3" "$VIEJOS"

# Regla 13: identificadores ASCII. Los comentarios si llevan tilde.
NO_ASCII=$(grep -rn '' "$SRC" --include='*.java' 2>/dev/null | solo_codigo \
    | LC_ALL=C grep '[^ -~]' || true)
comprobar "identificadores ASCII" "$NO_ASCII"

printf '\n'
if [ "$FALLOS" -eq 0 ]; then
    echo "== msvc-$CTX: gate S3 limpio"
else
    echo "== msvc-$CTX: $FALLOS fallo(s)"
fi
exit "$FALLOS"
