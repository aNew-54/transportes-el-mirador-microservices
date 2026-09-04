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

---

## Slice `S1-dominio` — decisiones de diseño

Sólo dominio y pruebas. Sin `@Entity`, sin repositorios, sin controladores, sin Feign, sin migraciones.
Rigen las ocho **reglas de dominio** de [`../README.md`](../README.md#6-reglas-de-dominio).

Es el slice con más invariantes después del Core: nueve, repartidas en dos agregados que no se tocan entre sí.

### Correspondencia con el diseño táctico (regla 13)

| Diseño táctico | Código |
|---|---|
| `EjecuciónDeViaje` | `EjecucionDeViaje` |
| `LiquidaciónDeViaje` | `LiquidacionDeViaje` |
| `EstadoDeEjecución` | `EstadoDeEjecucion` |
| `ConformidadDeEntrega` | igual |
| `EstadoDeLiquidación` | `EstadoDeLiquidacion` |

El tiempo se modela con `OffsetDateTime`: hitos, esperas y conformidades son instantes, no días.

### Objetos de valor — `models/vo`

| Tipo | Forma | Comportamiento |
|---|---|---|
| `Dinero` | `record Dinero(BigDecimal monto, String codigoMoneda)` | No negativo, escala 2, ISO-4217. `sumar`, `restar`, `esMayorQue`. Monedas distintas lanzan |
| `Saldo` | `record Saldo(Dinero importe, SignoDeSaldo signo)` | **Nunca se persiste** (**LIQ-02**). Fábrica `Saldo.entre(Dinero anticipo, Dinero gastos)`: `A_FAVOR_DEL_CONDUCTOR` si los gastos superan el anticipo, `A_FAVOR_DE_LA_EMPRESA` si sobra anticipo, `SALDADO` si coinciden |
| `Evidencia` | `record Evidencia(List<String> fotografias, String descripcion, OffsetDateTime momento)` | Al menos una fotografía y descripción no vacía. Lista inmutable |
| `EsperaFacturable` | `record EsperaFacturable(OffsetDateTime inicio, OffsetDateTime fin, int tiempoLibreHoras)` | `fin` posterior a `inicio`. **`excedente()`** devuelve sólo las horas que superan el tiempo libre pactado, o cero. Es lo que viaja en los contratos 7 y 8 |
| `Comprobante` | `record Comprobante(String tipo, String numero, OffsetDateTime fecha)` | Los tres obligatorios y no vacíos. Es lo que exige **LIQ-01** |
| `ResultadoDeCheckList` | `record ResultadoDeCheckList(boolean aprobado, List<String> observaciones, OffsetDateTime momento)` | No aprobado exige observaciones no vacías |

Enumeraciones: `EstadoDeEjecucion` (`PENDIENTE` · `EN_RUTA` · `SUSPENDIDA` · `ENTREGADA` · `CERRADA`),
`EstadoDeParada` (`PENDIENTE` · `EN_SITIO` · `ATENDIDA`), `TipoDeHito` (`SALIDA` · `PASO_DE_CONTROL` ·
`LLEGADA_A_PARADA` · `INICIO_DE_DESCARGA` · `FIN_DE_DESCARGA` · `LLEGADA_A_DESTINO`), `TipoDeIncidencia`
(`DANIO` · `FALTANTE` · `RECHAZO_DE_CARGA` · `AVERIA` · `DEMORA` · `CLIMA` · `BLOQUEO_DE_VIA`),
`EstadoConformidad` (`PENDIENTE` · `FIRMADA` · `OBSERVADA`), `ConceptoDeGasto` (`COMBUSTIBLE` · `PEAJE` ·
`VIATICO` · `COCHERA` · `IMPREVISTO`), `EstadoDeLiquidacion` (`ABIERTA` · `OBSERVADA` · `APROBADA`),
`SignoDeSaldo` (`A_FAVOR_DEL_CONDUCTOR` · `A_FAVOR_DE_LA_EMPRESA` · `SALDADO`).

`DANIO` en vez de `DAÑO` por la regla 13.

**Las tres incidencias que exigen evidencia** son `DANIO`, `FALTANTE` y `RECHAZO_DE_CARGA`.
`TipoDeIncidencia.exigeEvidencia()` lo decide, y es la misma lista que bloquea la emisión en Facturación
(FAC-05).

**Transiciones permitidas de `EstadoDeEjecucion`:**

| Desde | Hacia |
|---|---|
| `PENDIENTE` | `EN_RUTA` |
| `EN_RUTA` | `SUSPENDIDA` · `ENTREGADA` |
| `SUSPENDIDA` | `EN_RUTA` |
| `ENTREGADA` | `CERRADA` |
| `CERRADA` | — (terminal) |

### Agregado `EjecucionDeViaje` — `models/entity`

Raíz `EjecucionDeViaje`, **con la identidad del viaje**: el campo `id` es el `viajeId`. Entidades hijas
`CheckListDeSalida`, `Parada`, `ConformidadDeEntrega`, `Hito`, `Incidencia`.

Campos: `viajeId`, `unidadEjecutoraId`, `EstadoDeEjecucion`, `CheckListDeSalida` (nulo hasta registrarlo),
`List<Parada>`, `List<Hito>`, `List<Incidencia>`, `List<String> unidadesAnteriores`.

| Método | Contrato |
|---|---|
| `crear(String viajeId, String unidadEjecutoraId, List<Parada>)` | Nace `PENDIENTE`. Las paradas vienen de la hoja de ruta (contrato 4); Ejecución es `Conformist` y no las decide |
| `registrarCheckList(ResultadoDeCheckList)` | Sólo en `PENDIENTE` |
| `iniciar(OffsetDateTime)` | **EJV-01**: sin check-list registrado **o** con check-list no aprobado, lanza `CheckListNoAprobadoException`. Pasa a `EN_RUTA` |
| `reportarHito(Hito)` | **EJV-04**: sobre una ejecución `ENTREGADA` o `CERRADA` lanza `EjecucionEntregadaException` |
| `registrarIncidencia(Incidencia)` | Si `tipo.exigeEvidencia()` y la evidencia falta, lanza `EvidenciaRequeridaException` |
| `registrarConformidad(int secuenciaDeParada, ConformidadDeEntrega)` | **EJV-02**: la conformidad pertenece a la **parada**, y cada parada corresponde a una orden de servicio. Registrar dos conformidades en la misma parada lanza. Reabrir una parada `ATENDIDA` lanza (**EJV-04**) |
| `marcarEntregada(OffsetDateTime)` | **EJV-03**: si alguna parada no tiene conformidad `FIRMADA`, lanza `ConformidadesPendientesException`. Sólo entonces pasa a `ENTREGADA` |
| `transbordar(String nuevaUnidadId)` | **EJV-05**: cambia `unidadEjecutoraId` y apila la anterior en `unidadesAnteriores`. **Conserva el `viajeId`**: no se crea una ejecución nueva. Sobre `ENTREGADA` o `CERRADA` lanza |
| `cerrar(boolean hayLiquidacionesPendientes)` | **LIQ-04**: con liquidaciones pendientes lanza `LiquidacionPendienteException`. El parámetro es **obligatorio** (regla D2): la liquidación vive en otro agregado y no se asume que esté aprobada |
| `incidenciasSinResolver()` | Las que exigen evidencia y siguen abiertas. Alimenta el contrato 8 y FAC-05 |

Entidades hijas:

- `Parada`: `secuencia`, `ordenDeServicioId`, `direccion`, `EstadoDeParada`, `ConformidadDeEntrega` (nula
  hasta firmarse), `EsperaFacturable` (nula). **Inmutable en su itinerario**: no hay método que cambie
  secuencia, dirección ni orden. Si el itinerario cambia, Programación emite una hoja de ruta nueva.
- `ConformidadDeEntrega`: `id`, `ordenDeServicioId`, `EstadoConformidad`, `recibidoPor`, `OffsetDateTime`,
  `observaciones`.
- `Hito`: `id`, `TipoDeHito`, `OffsetDateTime`, `ubicacion`.
- `Incidencia`: `id`, `TipoDeIncidencia`, `descripcion`, `Evidencia` (nula si el tipo no la exige),
  `boolean resuelta`, `OffsetDateTime`.
- `CheckListDeSalida`: `id`, `ResultadoDeCheckList`.

### Agregado `LiquidacionDeViaje` — `models/entity`

Raíz `LiquidacionDeViaje`, entidad hija `GastoDeRuta`. **Identidad compuesta `viajeId` + `conductorId`**:
en un viaje con relevo hay dos liquidaciones independientes.

Campos: `viajeId`, `conductorId`, `Dinero anticipo`, `List<GastoDeRuta>`, `EstadoDeLiquidacion`,
`OffsetDateTime fechaDeAprobacion`.

| Método | Contrato |
|---|---|
| `abrir(String viajeId, String conductorId, Dinero anticipo)` | Nace `ABIERTA` |
| `rendirGasto(GastoDeRuta)` | **LIQ-01**: sin comprobante lanza `GastoSinComprobanteException`. **LIQ-03**: sobre una `APROBADA` lanza `LiquidacionAprobadaException` |
| `totalDeGastos()` | Suma de los gastos. **D8: se calcula** |
| `saldo()` | **LIQ-02**: `Saldo.entre(anticipo, totalDeGastos())`. **Se calcula siempre y no se persiste.** No existe campo `saldo` ni setter |
| `aprobar(OffsetDateTime)` | **LIQ-03**: aprobar una ya `APROBADA` lanza. Pasa a `APROBADA` |
| `observar(String motivo)` | De `ABIERTA` a `OBSERVADA`. Sobre `APROBADA` lanza |
| `estaPendiente()` | `true` mientras no esté `APROBADA`. Es lo que consulta **LIQ-04** |

`GastoDeRuta` — entidad hija: `id`, `ConceptoDeGasto`, `Dinero importe`, `Comprobante` **obligatorio**,
`descripcion`. El comprobante obligatorio en el constructor hace **LIQ-01** inexpresable de otro modo.

### Excepciones — `exceptions`

Raíz `DominioEjecucionException`; herederas `MonedaIncompatibleException`, `CheckListNoAprobadoException`,
`EjecucionEntregadaException`, `ConformidadesPendientesException`, `EvidenciaRequeridaException`,
`GastoSinComprobanteException`, `LiquidacionAprobadaException`, `LiquidacionPendienteException`,
`TransicionDeEjecucionInvalidaException`.

### Pruebas exigidas por este slice

| Invariante | Prueba mínima |
|---|---|
| **EJV-01** | `iniciar` sin check-list lanza. Con check-list **no aprobado** lanza. Con check-list aprobado pasa a `EN_RUTA`. Son tres casos, no uno |
| **EJV-02** | Viaje con tres paradas de tres órdenes distintas: se registran tres conformidades, una por parada. Dos conformidades en la misma parada lanzan |
| **EJV-03** | Con dos de tres conformidades firmadas, `marcarEntregada` lanza y el estado sigue `EN_RUTA`. Con las tres, pasa a `ENTREGADA` |
| **EJV-04** | Sobre una ejecución `ENTREGADA`: `reportarHito` lanza y reabrir una parada `ATENDIDA` lanza |
| **EJV-05** | Transbordo: `unidadEjecutoraId` cambia, `viajeId` **no**, y la unidad anterior queda registrada |
| **LIQ-01** | `GastoDeRuta` sin comprobante no se puede construir; `rendirGasto` con comprobante incompleto lanza |
| **LIQ-02** | `saldo()` se recalcula tras rendir un gasto nuevo. No existe campo ni setter de saldo — se comprueba por reflexión que la clase no declara un campo llamado `saldo` |
| **LIQ-03** | Sobre una liquidación `APROBADA`: `rendirGasto`, `aprobar` y `observar` lanzan |
| **LIQ-04** | `cerrar(true)` lanza; `cerrar(false)` sobre una ejecución `ENTREGADA` pasa a `CERRADA` |

Bordes obligatorios:

- Viaje con relevo: dos liquidaciones con el mismo `viajeId` y distinto `conductorId`, independientes; aprobar
  una no aprueba la otra.
- Incidencia de `DANIO`, `FALTANTE` y `RECHAZO_DE_CARGA` sin evidencia: las tres lanzan. `DEMORA` sin
  evidencia no lanza.
- `EsperaFacturable.excedente()` con espera menor, igual y mayor que el tiempo libre: cero, cero y la
  diferencia. El borde exacto se prueba (regla D5).
- `Saldo.entre` en los tres signos, incluido el caso exacto `SALDADO`.
- Las transiciones prohibidas de `EstadoDeEjecucion`, una a una.
- Toda operación con fecha nula lanza `IllegalArgumentException`.

### Correcciones tras la revisión de `S1-dominio`

**`EsperaFacturable.excedente()` devolvía `double`.** Calculaba con `BigDecimal` y tiraba la precisión con
`.doubleValue()` en la última línea. Ese valor viaja en los contratos 7 y 8 y allí se multiplica por una
tarifa horaria: es un importe en potencia. En coma flotante binaria, 0,1 hora facturada mil veces no suma
cien. Pasa a `BigDecimal`, y con él `tiempoRealHoras()`.

Se acepta que el agente añadiera `EjecucionDeViaje.reabrirParada(int)` y `Parada.reabrir()`, que la spec no
pedía: sin una vía para reabrir una parada atendida, **EJV-04 no se puede violar**, y una invariante que no
se puede violar tampoco se puede probar. Queda incorporado a la spec.

`Parada.estaConforme()` usa `conformidad != null && conformidad.estaFirmada()`, que parece un D2 pero no lo
es: es un predicado positivo, y la ausencia de conformidad da `false`. Falla cerrado.

### Notas de `S2-persistencia`

Es el contexto con los mapeos más incómodos, y de aquí salieron tres piezas de la receta:

- **`Evidencia` y `ResultadoDeCheckList` dejan de ser `record`.** Los dos poseen una colección, y un
  `record` no puede recibirla: Hibernate lo construye entero por el constructor canónico y sólo después
  rellena las colecciones. Pasan a clase inmutable, con la misma API hacia fuera.
- **`Parada` gana una clave sustituta.** Su identidad de negocio es la secuencia dentro de la ejecución,
  que no es única en la tabla. El `Long` generado es una concesión al mapeo y el dominio no lo ve: no
  hay accesor.
- **`LiquidacionDeViaje` lleva clave compuesta** `viajeId` + `conductorId` con `@IdClass`. No es un
  capricho del mapeo: en un viaje con relevo hay dos liquidaciones sobre el mismo viaje, y la prueba de
  integración las guarda las dos y comprueba que aprobar una no aprueba la otra.

**LIQ-02 se comprueba ahora en dos niveles.** La prueba de dominio recorre `getDeclaredFields()` y exige
que no exista un campo `saldo`; la de integración lee los metadatos de la tabla `liquidaciones` y exige
que no exista una columna `saldo`. Una invariante que dice «nunca se almacena» tiene que verificarse
también donde se almacena.
