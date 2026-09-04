# msvc-ejecucion — Ejecución y Seguimiento

| | |
|---|---|
| Bounded context | Ejecución y Seguimiento |
| Subdominio | Support |
| Puerto | `8030` |
| Esquema | `mirador_ejecucion` |
| Paquete raíz | `pe.edu.unc.elmirador.ejecucion` |
| Responsable de revisión | Alexander Infante |
| Agregados | 2 |
| Invariantes | 9 (EJV-01…05, LIQ-01…04) |

## Responsabilidad

Registra lo que efectivamente ocurre desde que el viaje sale a ruta: check-list de salida, verificación de
carga en origen, hitos, incidencias, conformidades de entrega y liquidación de gastos del conductor.

Es el reflejo de la realidad operativa frente a lo que Programación planificó. Ejecución **no decide**: es
`Conformist` respecto de la hoja de ruta.

Es también el servicio con más salidas: alimenta a Unidades, Conductores, Comercial y Facturación.

## Agregados

### `EjecuciónDeViaje` — raíz `EjecuciónDeViaje`

Comparte identidad con el viaje planificado (`ViajeId`).

- **Entidades hijas**: `CheckListDeSalida`, `Parada`, `ConformidadDeEntrega`, `Hito`, `Incidencia`
- **Objetos de valor**: `EstadoDeEjecución`, `EstadoDeParada`, `ResultadoDeCheckList`, `TipoDeHito`,
  `TipoDeIncidencia`, `Evidencia`, `EsperaFacturable`, `EstadoConformidad`
- **Referencias**: `ViajeId` (identidad), `UnidadId` ejecutora, `OrdenDeServicioId`
- **Métodos**: `iniciar()`, `reportarHito(hito)`, `registrarIncidencia(inc)`, `transbordar(unidad)`
- **Invariantes**: EJV-01 … EJV-05

`EstadoDeEjecución` ∈ `PENDIENTE` · `EN_RUTA` · `SUSPENDIDA` · `ENTREGADA` · `CERRADA`.
`EstadoDeParada` ∈ `PENDIENTE` · `EN_SITIO` · `ATENDIDA`.

**La conformidad pertenece a la parada, no directamente al viaje.** Un viaje con carga de tres clientes
recoge tres conformidades, una por orden de servicio (EJV-02), y sólo pasa a `ENTREGADA` cuando las tres
están firmadas (EJV-03).

`Evidencia` (fotografías, descripción, momento) es **obligatoria** cuando la incidencia es de daño, faltante
o rechazo de carga.

`EsperaFacturable.excedente()` calcula sólo la porción de tiempo que supera el tiempo libre pactado. Es lo
que viaja en el contrato 7 hacia Comercial y en el contrato 8 hacia Facturación.

Un transbordo (EJV-05) cambia `UnidadId` ejecutora **sin crear una ejecución nueva**: el viaje y la orden
siguen siendo los mismos.

### `LiquidaciónDeViaje` — raíz `LiquidaciónDeViaje`, entidad hija `GastoDeRuta`

Identidad compuesta por `ViajeId` + `ConductorId`. En un viaje con relevo hay **dos liquidaciones**, una por
cada conductor.

- **Objetos de valor**: `Anticipo`, `Saldo`, `ConceptoDeGasto`, `Comprobante`, `EstadoDeLiquidación`
- **Métodos**: `rendirGasto(gasto)`, `aprobar()`, `observar()`, `saldo()`
- **Invariantes**: LIQ-01 … LIQ-04

`ConceptoDeGasto` ∈ `COMBUSTIBLE` · `PEAJE` · `VIATICO` · `COCHERA` · `IMPREVISTO`.

`Saldo` (dinero + signo) **se calcula siempre en el momento y nunca se persiste** (LIQ-02). Una columna
`saldo` en la tabla es un defecto.

## API pública `/api/v1`

| Método | Ruta | Qué hace | Códigos |
|---|---|---|---|
| `POST` | `/ejecuciones` | Crea la ejecución desde la hoja de ruta | `201` `404` `409` `503` |
| `POST` | `/ejecuciones/{viajeId}/checklist` | Registra el resultado del check-list | `200` `409` |
| `POST` | `/ejecuciones/{viajeId}/iniciar` | Pasa a `EN_RUTA` | `200` `409` (EJV-01) |
| `POST` | `/ejecuciones/{viajeId}/hitos` | Reporta un hito | `201` `409` (EJV-04) |
| `POST` | `/ejecuciones/{viajeId}/incidencias` | Registra una incidencia con evidencia | `201` `400` `409` |
| `POST` | `/ejecuciones/{viajeId}/transbordo` | Cambia la unidad ejecutora | `200` `409` |
| `POST` | `/ejecuciones/{viajeId}/paradas/{secuencia}/conformidad` | Registra la conformidad del cliente | `201` `409` |
| `POST` | `/ejecuciones/{viajeId}/cerrar` | Cierra la ejecución | `200` `409` (LIQ-04) |
| `GET` | `/ejecuciones/{viajeId}` | Consulta el estado real del viaje | `200` `404` |
| `POST` | `/liquidaciones` | Abre la liquidación con su anticipo | `201` `409` |
| `POST` | `/liquidaciones/{viajeId}/{conductorId}/gastos` | Rinde un gasto con comprobante | `201` `422` (LIQ-01) |
| `POST` | `/liquidaciones/{viajeId}/{conductorId}/aprobar` | Aprueba la liquidación | `200` `409` |

## API interna `/internal/v1`

Ejecución **no publica** endpoints de integración. Es siempre el lado que llama: empuja los datos hacia los
cuatro servicios que los necesitan.

## Clientes Feign que consume

| Cliente | Servicio | Contrato | Propiedad |
|---|---|---|---|
| `ProgramacionClient` | Programación | 4 | `clients.programacion.url` |
| `UnidadesClient` | Unidades | 5 | `clients.unidades.url` |
| `ConductoresClient` | Conductores | 6 | `clients.conductores.url` |
| `ComercialClient` | Comercial | 7 | `clients.comercial.url` |
| `FacturacionClient` | Facturación | 8 | `clients.facturacion.url` |

Los cinco `POST` de reporte envían `Idempotency-Key`. Un reintento no duplica kilometraje, horas ni conformidad.

El contrato 8 es **crítico**: si la conformidad no llega, Facturación queda bloqueada. Un fallo de esa llamada
se registra y se reintenta; no se descarta en silencio.

## Criterios de éxito

- [ ] `./mvnw -pl msvc-ejecucion verify` en verde (exige Docker: levanta MySQL con Testcontainers)
- [ ] Cada tabla del contexto creada por una migración Flyway; `ddl-auto=validate` en verde
- [ ] `PersistenciaEjecucionIT` en verde contra MySQL real
- [ ] Las 9 invariantes con prueba que las viola
- [ ] `LiquidaciónDeViaje` **sin** columna de saldo persistida; prueba de que `saldo()` se calcula
- [ ] Prueba de viaje con tres órdenes: no pasa a `ENTREGADA` con dos conformidades firmadas (EJV-03)
- [ ] Prueba de transbordo: cambia `UnidadId` y conserva el mismo `ViajeId` (EJV-05)
- [ ] Prueba de incidencia de daño sin evidencia: rechazada con `400`
- [ ] Prueba de viaje con relevo: dos liquidaciones independientes
- [ ] Los 5 clientes Feign con timeout, traducción de error y prueba con stub
- [ ] Idempotencia probada: dos `POST` con la misma clave producen un solo efecto
- [ ] 0 imports de otro contexto
- [ ] Sano en `./scripts/smoke-test.sh`
