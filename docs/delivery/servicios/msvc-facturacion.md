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

---

## Slice `S1-dominio` — decisiones de diseño

Sólo dominio y pruebas. Sin `@Entity`, sin repositorios, sin controladores, sin Feign, sin migraciones.
Los objetos de valor llevan `@Embeddable`.

Rigen las ocho **reglas de dominio** de [`../README.md`](../README.md#6-reglas-de-dominio).

### Correspondencia con el diseño táctico (regla 13)

| Diseño táctico | Código |
|---|---|
| `NúmeroDeComprobante` | `NumeroDeComprobante` |
| `Detracción` | `Detraccion` |
| `LíneaDeFactura` | `LineaDeFactura` |
| `NotaDeCrédito` | `NotaDeCredito` |
| `montoNeto()` | igual |

### Objetos de valor — `models/vo`

| Tipo | Forma | Comportamiento |
|---|---|---|
| `Dinero` | `record Dinero(BigDecimal monto, String codigoMoneda)` | No negativo, escala 2, ISO-4217. `sumar`, `restar`, `porcentaje(BigDecimal)`, `esCero()`, `esMayorQue`. Monedas distintas lanzan |
| `NumeroDeComprobante` | `record NumeroDeComprobante(String serie, int correlativo)` | Serie `F` o `B` más tres dígitos (`F001`). Correlativo positivo. `siguiente()` devuelve el correlativo + 1 en la misma serie. `formateado()` da `F001-00000310` |
| `Detraccion` | `record Detraccion(BigDecimal porcentaje, Dinero monto, String cuentaBancaria)` | Porcentaje en `[0, 100)`. Si el porcentaje es cero, el monto es cero y la cuenta puede faltar; si es mayor que cero, la cuenta bancaria es obligatoria. `montoNeto(Dinero total)` devuelve `total − monto` |
| `ConceptoFacturable` | enum | `FLETE` · `ESTIBA` · `CUSTODIA` · `SEGURO` · `ESPERA` · `REAJUSTE` · `FALSO_FLETE` |
| `EstadoDeFactura` | enum | `BLOQUEADA` · `EMITIDA` · `ANULADA` |
| `MotivoDeAjuste` | enum | `DANIO` · `FALTANTE` · `RECHAZO` · `ERROR_DE_FACTURACION` |
| `SnapshotComercial` | `record SnapshotComercial(String ordenDeServicioId, String clienteId, Dinero tarifa, String codigoMoneda, OffsetDateTime obtenidoEn)` | La respuesta del contrato 9 tal como llegó. **Inmutable y local**: un cambio posterior de tarifa en Comercial no altera una factura emitida |
| `Conformidad` | `record Conformidad(boolean registrada, List<String> incidenciasSinResolver, OffsetDateTime recibidaEn)` | Lo que llega por el contrato 8. Lista inmutable, nunca nula. `bloqueaEmision()` ⇔ no registrada **o** incidencias no vacías |

`DANIO` en vez de `DAÑO` por la regla 13: la eñe no es ASCII.

### Agregado `Factura` — `models/entity`

Raíz `Factura`, entidad hija `LineaDeFactura`.

Campos: `id`, `ordenDeServicioId`, `clienteId`, `NumeroDeComprobante` (nulo mientras esté `BLOQUEADA`),
`SnapshotComercial`, `Detraccion`, `Conformidad`, `EstadoDeFactura`, `List<LineaDeFactura>`,
`OffsetDateTime fechaDeEmision`, `boolean falsoFlete`.

| Método | Contrato |
|---|---|
| `abrir(...)` (fábrica) | Nace `BLOQUEADA`, sin número de comprobante y con `Conformidad` no registrada. `ordenDeServicioId` obligatorio y **final** (**FAC-02**) |
| `agregarLinea(LineaDeFactura)` | **FAC-03**: sobre una factura `EMITIDA` o `ANULADA` lanza `FacturaInmutableException` |
| `registrarConformidad(Conformidad)` | Lo que empuja Ejecución por el contrato 8. **FAC-03**: sobre una emitida lanza |
| `total()` | Suma de las líneas. **D8: se calcula** |
| `montoNeto()` | `detraccion.montoNeto(total())` |
| `emitir(NumeroDeComprobante, OffsetDateTime)` | **FAC-01**: sin conformidad registrada y sin falso flete lanza `EmisionSinConformidadException`. **FAC-05**: con `incidenciasSinResolver` no vacía lanza `IncidenciaSinResolverException` y la factura **sigue** `BLOQUEADA`. **FAC-04**: si `montoNeto + detraccion.monto ≠ total`, lanza `ImportesInconsistentesException`. **FAC-03**: emitir una ya emitida lanza |
| `emitirFalsoFlete(NumeroDeComprobante, OffsetDateTime)` | **FAC-01**, segunda mitad: la cancelación posterior al despacho se factura **sin** conformidad. Exige una única línea de concepto `FALSO_FLETE` |
| `anular(OffsetDateTime)` | De `EMITIDA` a `ANULADA` |
| `correspondeA(String ordenDeServicioId)` | **FAC-02** |
| `saldoAjustable()` | Total menos las notas de crédito ya aplicadas. Alimenta **NCR-01** |

`LineaDeFactura` — entidad hija: `id`, `ConceptoFacturable`, `descripcion`, `Dinero importe`.

**FAC-02, alcance honesto.** El dominio garantiza que una factura referencia **una sola** orden, obligatoria
e inmutable, y que una línea de otra orden se rechaza. La unicidad global —que no existan dos facturas para
la misma orden— es un índice único más la comprobación del repositorio, y se cierra en `S2`. Aquí se prueba
la parte que el agregado puede sostener; el `409` del endpoint es de `S3`.

**El número de comprobante no se autoasigna.** `emitir` lo recibe. La ausencia de saltos en la numeración
exige una secuencia transaccional, que es `S2`, y `NumeroDeComprobante.siguiente()` deja el cálculo listo.

### Agregado `NotaDeCredito` — `models/entity`

Campos: `id`, `facturaId`, `MotivoDeAjuste`, `Dinero monto`, `OffsetDateTime fechaDeEmision`, `motivoDetalle`.

| Método | Contrato |
|---|---|
| `emitir(String facturaId, MotivoDeAjuste, Dinero monto, Dinero saldoAjustableDeLaFactura, OffsetDateTime)` | **NCR-01**: si el monto excede el saldo ajustable, lanza `MontoExcedeElSaldoException`. `saldoAjustableDeLaFactura` es **obligatorio** (regla D2): sin él no hay contra qué comparar y no se asume que quepa |

### Excepciones — `exceptions`

Raíz `DominioFacturacionException`; herederas `MonedaIncompatibleException`, `FacturaInmutableException`,
`EmisionSinConformidadException`, `IncidenciaSinResolverException`, `ImportesInconsistentesException`,
`MontoExcedeElSaldoException`, `NumeroDeComprobanteInvalidoException`.

### Pruebas exigidas por este slice

| Invariante | Prueba mínima |
|---|---|
| **FAC-01** | Emitir sin conformidad registrada lanza. Con conformidad registrada y sin incidencias, emite. `emitirFalsoFlete` sin conformidad **sí** emite: es la otra mitad de la invariante |
| **FAC-02** | `ordenDeServicioId` es obligatorio en la fábrica y no existe método que lo cambie. `correspondeA` distingue. Una línea con otro `ordenDeServicioId` se rechaza |
| **FAC-03** | Sobre una factura `EMITIDA`: `agregarLinea`, `registrarConformidad` y `emitir` lanzan `FacturaInmutableException`. La corrección exige nota de crédito |
| **FAC-04** | Con `detraccion.monto` que no cuadra con `total − montoNeto`, `emitir` lanza. Con importes exactos —total 1821.60, detracción 4 % de 72.86, neto 1748.74— emite |
| **FAC-05** | Con `incidenciasSinResolver` de un elemento, `emitir` lanza y el estado **sigue** `BLOQUEADA`. Con la lista vacía, emite |
| **NCR-01** | Nota de crédito por encima del saldo ajustable lanza. Por el saldo exacto no lanza. Con `saldoAjustable` nulo lanza |

Bordes obligatorios:

- Tres órdenes, tres facturas: cada una con su `ordenDeServicioId`, ninguna comparte número de comprobante.
- `NumeroDeComprobante` con serie `F01`, `FF001` o correlativo 0 lanza; `formateado()` rellena a ocho dígitos.
- `Detraccion` con porcentaje mayor que cero y sin cuenta bancaria lanza; con porcentaje cero y monto cero no.
- Dos notas de crédito sucesivas: la segunda se compara contra el saldo ya reducido por la primera.
- El snapshot comercial no cambia tras emitir: no hay método que lo sustituya.
- Toda operación con fecha nula lanza `IllegalArgumentException`.

### Correcciones tras la revisión de `S1-dominio`

**FAC-02 era evadible.** `LineaDeFactura` traía un segundo constructor sin `ordenDeServicioId`, y
`agregarLinea` comprobaba `if (linea.ordenDeServicioId() != null && !coincide)`. Una línea construida con
el constructor corto llevaba `null` y entraba en cualquier factura. Es la regla **D2** literal, y la misma
forma que ya apareció en `msvc-unidades` con OMT-02.

`ordenDeServicioId` pasa a ser obligatorio en la línea y la comparación es incondicional.

Se aceptan las demás decisiones del agente: las tres sobrecargas de `NotaDeCredito.emitir` conservan el
instante y el saldo ajustable, así que no incumplen D1 ni D2; `aplicarNotaDeCredito` mantiene
`saldoAjustable()` como cálculo y no como campo; y `Factura.emitir()` compara el `montoNeto` recibido sin
deducirlo, que es lo que D3 exige.
