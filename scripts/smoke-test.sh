#!/bin/sh
#
# Levanta los siete y comprueba que los siete responden UP. Nada mas.
#
# El recorrido de extremo a extremo —una orden que llega hasta la cuenta por cobrar— es
# ./scripts/flujo-vertical.sh, que reusa el mismo arranque. No se pone aqui una version a medias:
# el primer intento enviaba una orden al contado, y una orden al contado NO consulta a Cobranza
# —esa es justo la segunda mitad de CLI-01—, asi que habria dado verde sin que saliera una sola
# peticion entre servicios.

set -eu

. "$(dirname "$0")/lib/servicios.sh"

trap detener_servicios EXIT INT TERM

arrancar_servicios

echo "Los siete microservicios iniciaron correctamente."
