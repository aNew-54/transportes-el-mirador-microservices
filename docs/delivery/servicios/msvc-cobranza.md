# msvc-cobranza — Cobranza

| | |
|---|---|
| Bounded context | Cobranza |
| Subdominio | Support |
| Puerto | `8070` |
| Esquema | `mirador_cobranza` |
| Paquete raíz | `pe.edu.unc.elmirador.cobranza` |
| Responsable de revisión | María Belén Vilca |
| Agregados | 2 |
| Invariantes | 5 (CCC-01/02/03, PAG-01/02) |

## Responsabilidad

Conciliar los pagos directos del cliente con los depósitos de detracción, gestionar la cartera vencida con sus
tramos de acción, suspender el crédito al superar los treinta días de atraso, e informar el estado crediticio
del cliente a Comercial.

Cierra el ciclo del negocio y lo realimenta: su salida hacia Comercial (contrato 11) decide si se acepta una
nueva orden a crédito.

## Agregados

### `CuentaCorrienteDelCliente` — raíz `CuentaCorrienteDelCliente`, entidad hija `CuentaPorCobrar`

La identidad es el cliente (`ClienteId`). Una cuenta corriente por cliente, con tantas cuentas por cobrar
como facturas a crédito pendientes.

- **Objetos de valor**: `EstadoCrediticio`, `DíasDeAtraso`, `EstadoDeDocumento`, `Dinero`
- **Referencias**: `ClienteId` (identidad), `FacturaId`
- **Métodos**: `evaluarCredito()`, `suspenderCredito()`, `rehabilitarCredito()`
- **Invariantes**: CCC-01, CCC-02, CCC-03

`EstadoDeDocumento` ∈ `VIGENTE` · `VENCIDA` · `CANCELADO`.

`DíasDeAtraso.tramoDeGestión()` devuelve la acción de cobranza que corresponde según el atraso. Los tramos
del negocio:

| Atraso | Acción |
|---|---|
| −5 días (antes del vencimiento) | `RECORDATORIO` |
| 1 a 15 días | `LLAMADA_DE_SEGUIMIENTO` |
| 16 a 30 días | `COMUNICACION_FORMAL` + informe a gerencia |
| más de 30 días | `SUSPENSION_DE_CREDITO` |

La suspensión es **automática** (CCC-01), no una decisión manual. Al superar los treinta días en cualquier
cuenta, el cliente pasa a `SUSPENDIDO` y desde ese momento sólo puede contratar al contado.

CCC-03 es la regla de la doble condición: una factura sujeta a detracción **no se cancela** mientras falte
cualquiera de los dos movimientos —el pago del cliente o el depósito de detracción en el Banco de la Nación—.
Marcarla como cancelada con uno solo es el error más probable de este servicio.

### `Pago` — raíz `Pago`, entidad hija `AplicaciónDePago`

- **Objetos de valor**: `MedioDePago`, `Dinero`
- **Referencias**: `DocumentoId`
- **Método**: `aplicarACuentaPorCobrar(cuenta, importe)`
- **Invariantes**: PAG-01, PAG-02

`MedioDePago` lleva `modalidad` y `referencia` (número de operación).

Un pago puede cubrir varias facturas, y una factura puede cobrarse en partes. Lo que no puede es aplicarse
por encima de su monto (PAG-01) ni a cuentas de otro cliente (PAG-02).

## API pública `/api/v1`

| Método | Ruta | Qué hace | Códigos |
|---|---|---|---|
| `GET` | `/cuentas-corrientes/{clienteId}` | Consulta la posición deudora completa | `200` `404` |
| `GET` | `/cuentas-por-cobrar` | Lista con filtro por cliente, estado y atraso | `200` |
| `POST` | `/pagos` | Registra un pago con su medio y referencia | `201` `400` |
| `POST` | `/pagos/{id}/aplicaciones` | Aplica el pago a una o varias cuentas | `201` `422` (PAG-01, PAG-02) |
| `POST` | `/cuentas-por-cobrar/{id}/detraccion` | Registra el depósito de detracción | `200` `409` |
| `POST` | `/cuentas-corrientes/{clienteId}/rehabilitar` | Rehabilita el crédito tras regularizar | `200` `409` |
| `GET` | `/cartera/gestion` | Cartera agrupada por tramo de gestión | `200` |

## API interna `/internal/v1`

Publica los contratos **10** y **11**.

| Método | Ruta | Consumidor | Contrato |
|---|---|---|---|
| `POST` | `/cuentas-por-cobrar` | Facturación | 10 |
| `GET` | `/clientes/{clienteId}/estado-crediticio` | Comercial | 11 |

El `POST` valida FAC-04 en la frontera: si `montoNeto + detraccion.monto ≠ total`, devuelve `422`. Cobranza no
corrige el importe recibido.

Sólo entran a la cartera las facturas a crédito. Las de contado se registran ya canceladas.

## Clientes Feign que consume

Ninguno. Cobranza es un proveedor puro.

## Criterios de éxito

- [ ] `./mvnw -pl msvc-cobranza verify` en verde (exige Docker: levanta MySQL con Testcontainers)
- [ ] Cada tabla del contexto creada por una migración Flyway; `ddl-auto=validate` en verde
- [ ] `PersistenciaCobranzaIT` en verde contra MySQL real
- [ ] Las 5 invariantes con prueba que las viola
- [ ] `DíasDeAtraso.tramoDeGestión()` probado en los cuatro bordes exactos: −5, 1, 15, 16, 30, 31
- [ ] Prueba de CCC-01: al cruzar los 30 días, el cliente queda `SUSPENDIDO` sin intervención manual
- [ ] Prueba de CCC-03: con pago recibido y sin detracción, la cuenta **no** queda `CANCELADO`; y a la inversa
- [ ] Prueba de PAG-01: la suma de aplicaciones no puede exceder el monto del pago
- [ ] Prueba de PAG-02: aplicar a la cuenta de otro cliente devuelve `422`
- [ ] Prueba de un pago que cubre dos facturas, y de una factura cobrada en dos pagos
- [ ] `POST /internal/v1/cuentas-por-cobrar` devuelve `422` si los importes no cuadran, e idempotencia probada
- [ ] 0 imports de otro contexto
- [ ] Sano en `./scripts/smoke-test.sh`
