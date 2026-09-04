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

---

## Slice `S1-dominio` — decisiones de diseño

Sólo modelo de dominio y pruebas. Sin `@Entity`, sin repositorios, sin controladores, sin migraciones.
Los objetos de valor llevan `@Embeddable`: la anotación es inerte mientras ninguna entidad los referencie.

### Reglas heredadas de las dos revisiones anteriores

Normativas, y son exactamente los tres defectos que se colaron en los slices previos pese a tener todas
las pruebas en verde:

1. **El dominio no lee el reloj.** Ni un `LocalDate.now()`. La fecha se recibe y se exige no nula.
2. **Ninguna invariante se evade pasando `null`.** Prohibido `if (x != null && !cumple) fallar;`, que deja
   pasar el caso nulo. Si el dato hace falta para evaluar la invariante, es obligatorio.
3. **Nada de valores por defecto silenciosos** en datos del negocio, la moneda incluida.
4. **Cuidado con los rangos inclusivos.** Un periodo `[desde, hasta]` inclusivo en ambos extremos convierte
   una ventana de un día en una de dos. Aquí importa en los tramos de gestión, que van por bordes exactos.
5. Identificadores ASCII (regla 13).

### Correspondencia con el diseño táctico

| Diseño táctico | Código |
|---|---|
| `DíasDeAtraso.tramoDeGestión()` | `DiasDeAtraso.tramoDeGestion()` |
| `evaluarCredito()` | `evaluarCredito(LocalDate)` |
| `aplicarACuentaPorCobrar(cuenta, importe)` | igual |
| `AplicaciónDePago` | `AplicacionDePago` |

### Objetos de valor — `models/vo`

| Tipo | Forma | Comportamiento |
|---|---|---|
| `Dinero` | `record Dinero(BigDecimal monto, String codigoMoneda)` | Monto no negativo, escala 2, ISO-4217 de 3 letras. `sumar`, `restar`, `esCero()`, `esMayorQue`. Operar monedas distintas lanza `MonedaIncompatibleException`. Moneda obligatoria: no se adivina |
| `DiasDeAtraso` | `record DiasDeAtraso(int dias)` | Negativo = aún no vence. `tramoDeGestion()`, `superaLosTreinta()`. Fábrica `entre(LocalDate vencimiento, LocalDate referencia)` |
| `EstadoCrediticio` | `record EstadoCrediticio(SituacionCrediticia situacion, String motivo, LocalDate fechaDeCambio)` | `permiteCredito()` ⇔ `VIGENTE`. Toda situación distinta exige motivo y fecha. Fábricas `vigente(fecha)`, `suspendido(motivo, fecha)` |
| `MedioDePago` | `record MedioDePago(ModalidadDePago modalidad, String referencia)` | `referencia` obligatoria salvo en `EFECTIVO` |

Enumeraciones:

| Enum | Valores |
|---|---|
| `SituacionCrediticia` | `VIGENTE` · `SUSPENDIDO` |
| `EstadoDeDocumento` | `VIGENTE` · `VENCIDA` · `CANCELADO` |
| `TramoDeGestion` | `SIN_ACCION` · `RECORDATORIO` · `LLAMADA_DE_SEGUIMIENTO` · `COMUNICACION_FORMAL` · `SUSPENSION_DE_CREDITO` |
| `ModalidadDePago` | `EFECTIVO` · `TRANSFERENCIA` · `DEPOSITO` · `CHEQUE` |

`situacion` usa los mismos dos valores que el contrato 11; no se inventa un tercero.

**Tabla de decisión de `tramoDeGestion()`.** Los bordes son exactos y hay que probarlos uno a uno:

| `dias` | Tramo |
|---|---|
| menor que −5 | `SIN_ACCION` |
| −5 a 0 | `RECORDATORIO` |
| 1 a 15 | `LLAMADA_DE_SEGUIMIENTO` |
| 16 a 30 | `COMUNICACION_FORMAL` |
| 31 o más | `SUSPENSION_DE_CREDITO` |

`superaLosTreinta()` es `dias > 30`, no `>= 30`. Es la condición literal de **CCC-01**.

### Agregado `CuentaCorrienteDelCliente` — `models/entity`

Raíz `CuentaCorrienteDelCliente`, entidad hija `CuentaPorCobrar`. **La identidad es el `clienteId`.**

Campos de la raíz: `clienteId`, `EstadoCrediticio estado`, `List<CuentaPorCobrar> cuentas`.

| Método | Contrato |
|---|---|
| `evaluarCredito(LocalDate fecha)` | **CCC-01**: si alguna cuenta no cancelada supera los treinta días de atraso, pasa a `SUSPENDIDO` con motivo. Es automático, no una decisión manual |
| `suspenderCredito(String motivo, LocalDate fecha)` | Suspensión manual |
| `rehabilitarCredito(LocalDate fecha)` | Falla con `RehabilitacionInvalidaException` si sigue habiendo una cuenta por encima de treinta días. No se rehabilita sobre una cartera vencida |
| `registrarCuenta(CuentaPorCobrar)` | Rechaza una cuenta de otro cliente y una `facturaId` ya registrada |
| `deudaTotal()` | Suma de saldos de las cuentas no canceladas |
| `diasDeAtrasoMaximo(LocalDate)` · `cuentasVencidas(LocalDate)` | Alimentan el contrato 11 |

`CuentaPorCobrar` — entidad hija. Campos: `id`, `clienteId`, `facturaId`, `documentoId`, `Dinero total`,
`Dinero detraccion`, `LocalDate fechaDeVencimiento`, `Dinero aplicado`, `boolean detraccionDepositada`.

| Método | Contrato |
|---|---|
| `montoNeto()` | `total − detraccion`. Lo que se cobra al cliente; la detracción la deposita él en el Banco de la Nación |
| `saldo()` | `montoNeto() − aplicado`. **No se almacena, se calcula.** **CCC-02**: nunca negativo, garantizado porque `aplicar` rechaza el exceso |
| `aplicar(Dinero importe)` | Suma al aplicado. Si excediera `montoNeto()` lanza `SaldoInsuficienteException` (**CCC-02**) |
| `registrarDepositoDeDetraccion()` | Marca el depósito. Sobre una cuenta sin detracción lanza |
| `estaCancelada()` | **CCC-03**, la doble condición: `saldo().esCero()` **y** (`detraccion.esCero()` **o** `detraccionDepositada`). Marcarla cancelada con uno solo de los dos movimientos es el error más probable de este servicio |
| `estadoEn(LocalDate)` | `CANCELADO`, `VENCIDA` o `VIGENTE` según la fecha y la cancelación |
| `diasDeAtraso(LocalDate)` | `DiasDeAtraso.entre(fechaDeVencimiento, fecha)` |

En el constructor se valida `montoNeto + detraccion == total` con los importes recibidos, que es **FAC-04**
verificada en la frontera del contrato 10. Cobranza no corrige el importe: lo rechaza.

### Agregado `Pago` — `models/entity`

Raíz `Pago`, entidad hija `AplicacionDePago`.

Campos: `id`, `clienteId`, `Dinero monto`, `MedioDePago`, `LocalDate fecha`, `List<AplicacionDePago>`.

| Método | Contrato |
|---|---|
| `aplicarACuentaPorCobrar(CuentaPorCobrar cuenta, Dinero importe)` | **PAG-02**: si `cuenta.clienteId()` no es el del pago, lanza `PagoDeOtroClienteException`. **PAG-01**: si la suma de aplicaciones excediera el monto del pago, lanza `AplicacionExcedeElPagoException`. Ninguna de las dos altera el estado al fallar |
| `montoAplicado()` · `saldoSinAplicar()` | Calculados, nunca almacenados |

`AplicacionDePago` — entidad hija: `id`, `cuentaPorCobrarId`, `Dinero importe`.

`aplicarACuentaPorCobrar` toca dos agregados en una operación: valida PAG-01 y PAG-02, registra la
aplicación en el pago y llama a `cuenta.aplicar(importe)`. Es el nombre que fija el diseño táctico y se
respeta. La transacción la abre el servicio de aplicación en `S3`; el dominio sólo garantiza que ninguna
de las dos partes queda a medias: **se valida todo antes de mutar nada.**

### Excepciones — `exceptions`

Raíz `DominioCobranzaException`; herederas `MonedaIncompatibleException`, `SaldoInsuficienteException`,
`PagoDeOtroClienteException`, `AplicacionExcedeElPagoException`, `RehabilitacionInvalidaException`,
`ImportesInconsistentesException`.

### Pruebas exigidas por este slice

| Invariante | Prueba mínima |
|---|---|
| **CCC-01** | Al cruzar los treinta días el cliente queda `SUSPENDIDO` sin intervención manual. En 30 días exactos **no** se suspende; en 31 sí |
| **CCC-02** | `aplicar` que dejaría el saldo negativo lanza y **no** altera el aplicado. Dejarlo exactamente en cero no lanza |
| **CCC-03** | Con pago completo y sin depósito de detracción, `estaCancelada()` es `false`. A la inversa, con detracción depositada y saldo pendiente, también `false`. Sólo con ambos es `true`. Una cuenta sin detracción se cancela sólo con el pago |
| **PAG-01** | La suma de aplicaciones no puede exceder el monto del pago; el intento fallido no altera el pago ni la cuenta |
| **PAG-02** | Aplicar a la cuenta de otro cliente lanza |

Bordes obligatorios:

- `tramoDeGestion()` en −6, −5, 0, 1, 15, 16, 30 y 31. Los ocho, uno a uno.
- `superaLosTreinta()` en 30 y en 31.
- Un pago que cubre dos facturas, y una factura cobrada en dos pagos.
- Constructor de `CuentaPorCobrar` con `montoNeto + detraccion ≠ total` lanza (FAC-04 en la frontera).
- `rehabilitarCredito` sobre una cartera con una cuenta de 45 días lanza.
- Toda operación con fecha nula lanza `IllegalArgumentException`.
- `Dinero` operando monedas distintas lanza.

### Correcciones tras la revisión de `S1-dominio`

**FAC-04 tenía una puerta trasera.** `CuentaPorCobrar` traía un tercer constructor, de siete parámetros,
que deducía `montoNeto = total − detraccion`. Por esa vía `montoNeto + detraccion == total` se cumple por
construcción y `ImportesInconsistentesException` no puede lanzarse nunca. Es exactamente lo que el contrato
10 debe rechazar con `422`, y 34 de las 35 llamadas de las pruebas usaban ese atajo.

El constructor se eliminó. `montoNeto` **se recibe, no se deduce**: es el tercer importe del contrato y
existe para contrastarlo. Cobranza rechaza los importes que no cuadran; no los corrige.

**`deudaTotal()` adivinaba la moneda y reventaba con la cartera vacía.** Tomaba `cuentas.get(0).total()
.codigoMoneda()` —un valor por defecto silencioso en un importe— y lanzaba `IllegalStateException` si no
había cuentas. Un cliente nuevo sin cartera es el primer caso que responde el contrato 11, y habría dado
`500`. Queda sólo `deudaTotal(String codigoMoneda)`.

`CuentaCorrienteSinCuentasTest` cubre la cartera vacía: deuda cero en cualquier moneda, sin suspensión y
con la moneda exigida.

**Decisiones de agy que se aceptan:**

- Orden de validación en `aplicarACuentaPorCobrar`: PAG-02, luego moneda, luego PAG-01, luego CCC-02, y
  sólo entonces muta. Cumple «se valida todo antes de mutar nada».
- `AplicacionDePago` genera su id como `<pagoId>-APP-<indice>`. Determinista y sin colisiones mientras no
  se puedan eliminar aplicaciones, que es el caso.
- `diasDeAtrasoMaximo` normaliza los atrasos negativos a cero: el contrato 11 informa mora, no anticipación.

**Nota sobre la evidencia:** estas dos correcciones son estructurales —un constructor de más y una
sobrecarga que adivinaba— y se ven en el diff. A diferencia del defecto de `msvc-conductores`, no hay una
prueba en rojo que las demuestre: el atajo no producía un resultado incorrecto, permitía saltarse la
comprobación.
