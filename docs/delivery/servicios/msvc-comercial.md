# msvc-comercial — Gestión Comercial

| | |
|---|---|
| Bounded context | Gestión Comercial |
| Subdominio | Support |
| Puerto | `8010` |
| Esquema | `mirador_comercial` |
| Paquete raíz | `pe.edu.unc.elmirador.comercial` |
| Responsable de revisión | Sarah Herrera |
| Agregados | 5 |
| Invariantes | 8 (CLI-01, COT-01/02, ORD-01/02, CTM-01/02, TAR-01) |

## Responsabilidad

Registrar la solicitud del cliente, cotizar según tarifario, generar la orden de servicio, administrar
contratos marco y condiciones de crédito, y gestionar cancelaciones con falso flete.

Es el punto de entrada del negocio: sin una orden confirmada aquí, Programación no puede planificar nada.

## Agregados

### `Cliente` — raíz `Cliente`

- **Objetos de valor**: `RUC`, `RazonSocial`, `CondicionDePago`, `EstadoCrediticio`
- **Método de negocio**: `puedeContratarACredito()`
- **Invariante**: CLI-01

`EstadoCrediticio` guarda `situacion` y `fechaDeCambio`, y se refresca desde el contrato 11 de Cobranza.
Es una copia local con marca de tiempo, no la fuente de verdad.

### `Cotización` — raíz `Cotización`

- **Objetos de valor**: `Carga`, `Ruta`, `Tarifa` (con `Recargo` y `Descuento`), `PeriodoDeVigencia`,
  `EstadoDeCotizacion`, `MotivoDeRechazo`
- **Referencias**: `ClienteId`, `TarifarioId`
- **Métodos**: `aceptar()`, `rechazar(motivo)`, `haVencido()`
- **Invariantes**: COT-01, COT-02

Vigencia de siete días calendario. `EstadoDeCotizacion` ∈ `EMITIDA` · `ACEPTADA` · `RECHAZADA` · `VENCIDA`.
`Descuento.porcentaje` debe mantenerse entre 5 % y 15 %.

### `OrdenDeServicio` — raíz `OrdenDeServicio`

- **Objetos de valor**: `Carga`, `Ruta`, `Tarifa`, `CondicionDePago`, `EstadoDeOrden`
- **Referencias**: `ClienteId`, `ContratoId`
- **Métodos**: `confirmar()`, `reajustarCarga(carga)`, `cancelar()`
- **Invariantes**: ORD-01, ORD-02

Una cancelación posterior al despacho genera **falso flete** por la mitad de la tarifa, y requiere
autorización de gerencia registrada.

### `ContratoMarco` — raíz `ContratoMarco`, entidad hija `TarifaPactada`

- **Objetos de valor**: `PeriodoDeVigencia`, `TiempoLibre`, `CláusulaDeConsolidación`
- **Referencias**: `ClienteId`
- **Invariantes**: CTM-01, CTM-02

`CláusulaDeConsolidación` lleva `permitida` y `restricciones`; es lo que Programación consume en el contrato 1
para sostener VIA-04. Vigencia de un año con revisión semestral.

### `Tarifario` — raíz `Tarifario`

- **Objetos de valor**: `PeriodoDeVigencia`, `Recargo`, `Dinero`
- **Método**: `tarifaPara(ruta, tipoUnidad)`
- **Invariante**: TAR-01

## API pública `/api/v1`

| Método | Ruta | Qué hace | Códigos |
|---|---|---|---|
| `POST` | `/clientes` | Registra un cliente | `201` `400` `409` |
| `GET` | `/clientes/{id}` | Consulta un cliente | `200` `404` |
| `POST` | `/cotizaciones` | Emite una cotización aplicando el tarifario vigente | `201` `400` `422` |
| `POST` | `/cotizaciones/{id}/aceptar` | Acepta y genera la orden de servicio | `200` `409` (COT-01) |
| `POST` | `/cotizaciones/{id}/rechazar` | Registra el motivo de rechazo | `200` `409` |
| `POST` | `/ordenes` | Crea una orden directa (cliente con contrato marco) | `201` `422` (ORD-02) |
| `POST` | `/ordenes/{id}/confirmar` | Confirma la orden y la habilita para programación | `200` `409` |
| `POST` | `/ordenes/{id}/cancelar` | Cancela; genera falso flete si ya se despachó | `200` `409` |
| `POST` | `/contratos-marco` | Registra un contrato marco | `201` `400` |
| `POST` | `/tarifarios` | Publica un tarifario y vence el anterior | `201` `409` (TAR-01) |

## API interna `/internal/v1`

Publica los contratos **1**, **7** y **9** de [`../../api/contracts.md`](../../api/contracts.md).

| Método | Ruta | Consumidor | Contrato |
|---|---|---|---|
| `GET` | `/ordenes/{ordenId}` | Programación | 1 |
| `POST` | `/ordenes/{ordenId}/diferencias-de-carga` | Ejecución | 7 |
| `POST` | `/ordenes/{ordenId}/esperas` | Ejecución | 7 |
| `GET` | `/ordenes/{ordenId}/snapshot-facturable` | Facturación | 9 |

## Clientes Feign que consume

| Cliente | Servicio | Contrato | Propiedad |
|---|---|---|---|
| `CobranzaClient` | Cobranza | 11 | `clients.cobranza.url` |

Ante indisponibilidad de Cobranza, una orden **a crédito** se rechaza con `503`. Una orden al contado procede.

## Criterios de éxito

- [ ] `./mvnw -pl msvc-comercial verify` en verde (exige Docker: levanta MySQL con Testcontainers)
- [ ] Cada tabla del contexto creada por una migración Flyway; `ddl-auto=validate` en verde
- [ ] `PersistenciaComercialIT` en verde contra MySQL real
- [ ] CLI-01 · COT-01 · COT-02 · ORD-01 · ORD-02 · CTM-01 · CTM-02 · TAR-01 con prueba que las viola
- [ ] Los 5 agregados existen con su raíz, entidades hijas y objetos de valor
- [ ] `Tarifa.total()` aplica recargos y luego descuento, en ese orden, con prueba de importe exacto
- [ ] Los 4 endpoints `/internal/v1` responden los códigos de la tabla
- [ ] `CobranzaClient` con timeout, traducción de error y prueba con stub que cubre el `503`
- [ ] 0 imports de otro contexto
- [ ] Sano en `./scripts/smoke-test.sh`
