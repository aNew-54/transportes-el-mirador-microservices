# msvc-facturacion — Facturación

| | |
|---|---|
| Bounded context | Facturación |
| Subdominio | Generic (facturación electrónica) |
| Puerto | `8060` |
| Esquema | `mirador_facturacion` |
| Paquete raíz | `pe.edu.unc.elmirador.facturacion` |
| Responsable de revisión | María Belén Vilca |
| Agregados | 2 |
| Invariantes | 6 (FAC-01…05, NCR-01) |

## Responsabilidad

Emitir la factura electrónica una vez registrada la conformidad de entrega —o el falso flete en cancelaciones
posteriores al despacho—, calcular esperas y reajustes por diferencias de carga, y aplicar el régimen de
detracciones sobre el monto facturado.

Es `Conformist` en dos direcciones: consume la tarifa de Comercial **sin renegociarla**, y su formato de
comprobante lo impone SUNAT.

Regla de negocio que define la granularidad: **una factura por orden de servicio, no por viaje**. Un viaje con
carga de tres clientes emite tres facturas, porque cada cliente es un contribuyente distinto.

## Agregados

### `Factura` — raíz `Factura`, entidad hija `LíneaDeFactura`

- **Objetos de valor**: `NúmeroDeComprobante`, `Detracción`, `ConceptoFacturable`, `EstadoDeFactura`, `Dinero`
- **Referencias**: `OrdenDeServicioId`, `ClienteId`
- **Métodos**: `emitir()`, `anular()`
- **Invariantes**: FAC-01 … FAC-05

`EstadoDeFactura` ∈ `BLOQUEADA` · `EMITIDA` · `ANULADA`. Una factura nace `BLOQUEADA` y sólo pasa a `EMITIDA`
cuando llega la conformidad por el contrato 8 sin incidencias pendientes. Una vez `EMITIDA` es **inmutable**
(FAC-03).

`NúmeroDeComprobante` (serie + correlativo) **no admite saltos en la numeración**. La asignación del
correlativo debe ser transaccional respecto de la emisión.

`Detracción.montoNeto()` devuelve lo que se cobra directamente al cliente, descontando el porcentaje que este
deposita en la cuenta del Banco de la Nación. FAC-04 exige `montoNeto + detracción == total`.

`ConceptoFacturable` ∈ `FLETE` · `ESTIBA` · `CUSTODIA` · `SEGURO` · `ESPERA` · `REAJUSTE` · `FALSO_FLETE`.
Cada uno es una `LíneaDeFactura`.

**El snapshot comercial es local e inmutable.** Facturación persiste la respuesta del contrato 9 tal como
llegó y no la vuelve a consultar. Un cambio posterior de tarifa en Comercial no altera una factura emitida.

### `NotaDeCrédito` — raíz `NotaDeCrédito`

- **Objetos de valor**: `MotivoDeAjuste`, `Dinero`
- **Referencias**: `FacturaId`
- **Invariante**: NCR-01

`MotivoDeAjuste` ∈ `DAÑO` · `FALTANTE` · `RECHAZO` · `ERROR_DE_FACTURACION`.

Es el único mecanismo de corrección de una factura emitida.

## API pública `/api/v1`

| Método | Ruta | Qué hace | Códigos |
|---|---|---|---|
| `POST` | `/facturas` | Abre una factura `BLOQUEADA` para una orden | `201` `409` (FAC-02) `503` |
| `POST` | `/facturas/{id}/emitir` | Emite si hay conformidad y no hay incidencias | `200` `409` (FAC-01, FAC-05) `422` (FAC-04) |
| `POST` | `/facturas/{id}/anular` | Anula una factura | `200` `409` |
| `GET` | `/facturas/{id}` | Consulta la factura con sus líneas | `200` `404` |
| `GET` | `/facturas` | Lista con filtro por estado, cliente y fecha | `200` |
| `POST` | `/facturas/falso-flete` | Emite falso flete por cancelación tras el despacho | `201` `409` |
| `POST` | `/notas-de-credito` | Emite una nota de crédito sobre una factura | `201` `422` (NCR-01) |

## API interna `/internal/v1`

Publica el contrato **8**.

| Método | Ruta | Consumidor | Contrato |
|---|---|---|---|
| `POST` | `/conformidades` | Ejecución | 8 |

Este endpoint es el que desbloquea la emisión. Recibe el estado de la conformidad, los conceptos facturables
medidos en ruta y la lista de incidencias sin resolver. Con `incidenciasSinResolver` no vacío, la factura
permanece `BLOQUEADA` (FAC-05).

## Clientes Feign que consume

| Cliente | Servicio | Contrato | Propiedad |
|---|---|---|---|
| `ComercialClient` | Comercial | 9 | `clients.comercial.url` |
| `CobranzaClient` | Cobranza | 10 | `clients.cobranza.url` |

Si Comercial no responde al abrir la factura, la operación falla con `503`: sin snapshot no hay tarifa, y no
se inventa una.

Tras emitir, Facturación llama al contrato 10 con `Idempotency-Key = facturaId`. Un fallo de esa llamada deja
la factura emitida pero sin cuenta por cobrar; debe reintentarse, no ignorarse.

## Criterios de éxito

- [ ] `./mvnw -pl msvc-facturacion verify` en verde (exige Docker: levanta MySQL con Testcontainers)
- [ ] Cada tabla del contexto creada por una migración Flyway; `ddl-auto=validate` en verde
- [ ] `PersistenciaFacturacionIT` en verde contra MySQL real
- [ ] Las 6 invariantes con prueba que las viola
- [ ] Prueba de FAC-01: emitir sin conformidad devuelve `409`
- [ ] Prueba de FAC-02: dos facturas para la misma orden de servicio devuelve `409`
- [ ] Prueba de FAC-03: modificar una factura `EMITIDA` es imposible; la corrección exige nota de crédito
- [ ] Prueba de FAC-04 con importes exactos, no aproximados
- [ ] Prueba de FAC-05: `incidenciasSinResolver` no vacío mantiene la factura `BLOQUEADA`
- [ ] Prueba de viaje con tres órdenes: se emiten tres facturas, no una
- [ ] `NúmeroDeComprobante` sin saltos, probado con emisiones concurrentes
- [ ] Los 2 clientes Feign con timeout, traducción de error y prueba con stub que cubre el `503`
- [ ] 0 imports de otro contexto
- [ ] Sano en `./scripts/smoke-test.sh`
