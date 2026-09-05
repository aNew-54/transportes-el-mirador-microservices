# Dossier de sustentación

`dossier.html` es la página de documentación del proyecto, pensada para la sustentación.

Se abre sin servidor: basta con un doble clic. No tiene dependencias externas salvo las fuentes de
Google Fonts, y degrada a las tipografías del sistema si no hay red. Se adapta al tema claro y oscuro
del navegador.

## Qué contiene

| Sección | Qué muestra |
|---|---|
| Las 48 invariantes | Una celda por invariante, agrupadas por bounded context. Es el criterio de éxito del proyecto |
| Mapa de contexto | Los 11 contratos como matriz de adyacencia. Una fila vacía es un proveedor puro: sin OpenFeign |
| Verificación | Pruebas por módulo y las cuatro comprobaciones estructurales del gate |
| Hallazgos | Los treinta y cinco defectos que la revisión encontró pese a tener las pruebas en verde, repartidos por slice |
| Reglas destiladas | Las ocho reglas de dominio que salieron de esos defectos |
| Método | El reparto entre quien decide y quien escribe, y qué costó delegar |
| Estado | 48/48 invariantes, 32/32 slices, 11/11 contratos |

## Al actualizar las cifras

Los números están escritos a mano en el HTML. Tras integrar un slice nuevo hay que actualizar:

- la tira de cifras de la cabecera,
- la tabla de pruebas por módulo de la sección de verificación,
- las dos barras de avance de la sección de estado.

La fuente de verdad de esas cifras es `../delivery/backlog.md` y la salida de `./mvnw clean verify`.
