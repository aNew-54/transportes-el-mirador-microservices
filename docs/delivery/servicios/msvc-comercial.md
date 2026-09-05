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

---

## Slice `S1-dominio` — decisiones de diseño

Sólo dominio y pruebas. Sin `@Entity`, sin repositorios, sin controladores, sin Feign, sin migraciones.
Los objetos de valor llevan `@Embeddable`, inerte mientras ninguna entidad los referencie.

Rigen las ocho **reglas de dominio** de [`../README.md`](../README.md#6-reglas-de-dominio). No se repiten aquí.

### Correspondencia con el diseño táctico (regla 13)

| Diseño táctico | Código |
|---|---|
| `Cotización` | `Cotizacion` |
| `CláusulaDeConsolidación` | `ClausulaDeConsolidacion` |
| `RazónSocial` | `RazonSocial` |
| `haVencido()` | `haVencidoEn(LocalDate)` |
| `puedeContratarACredito()` | igual |

### Objetos de valor — `models/vo`

| Tipo | Forma | Comportamiento |
|---|---|---|
| `Ruc` | `record Ruc(String valor)` | Once dígitos; el primer par ∈ `10` `15` `17` `20`. Otro valor lanza `RucInvalidoException` |
| `RazonSocial` | `record RazonSocial(String valor)` | No vacía, se recorta, máximo 200 caracteres |
| `Dinero` | `record Dinero(BigDecimal monto, String codigoMoneda)` | Como en cobranza: no negativo, escala 2, ISO-4217. `sumar`, `restar`, `multiplicarPor(BigDecimal)`, `mitad()`, `esMayorQue`. Monedas distintas lanzan `MonedaIncompatibleException` |
| `PeriodoDeVigencia` | `record PeriodoDeVigencia(LocalDate desde, LocalDate hasta)` | `estaVigenteEn`, `seSolapaCon(otro)`, `diasDeVigencia()` |
| `Carga` | `record Carga(int pesoKg, BigDecimal volumenM3, TipoDeCarga tipo)` | Peso y volumen positivos |
| `Ruta` | `record Ruta(String origen, String destino, String corredor)` | Los tres obligatorios, normalizados a mayúsculas |
| `Recargo` | `record Recargo(TipoDeRecargo tipo, BigDecimal porcentaje)` | Porcentaje en `(0, 100]` |
| `Descuento` | `record Descuento(BigDecimal porcentaje, String autorizadoPor)` | **Entre 5 y 15 inclusive.** `autorizadoPor` obligatorio y no vacío: es la autorización de gerencia registrada que exige **COT-02** |
| `Tarifa` | `record Tarifa(Dinero base, List<Recargo> recargos, Descuento descuento)` | `total()`. `descuento` puede ser nulo; los recargos, lista vacía nunca nula |
| `CondicionDePago` | `record CondicionDePago(ModalidadDePago modalidad, int plazoEnDias)` | `CREDITO` exige plazo > 0; `CONTADO` exige plazo == 0. `esACredito()` |
| `EstadoCrediticio` | `record EstadoCrediticio(SituacionCrediticia situacion, LocalDate fechaDeCambio)` | Copia local del contrato 11, con marca de tiempo. `permiteCredito()`. **No es la fuente de verdad** |
| `TiempoLibre` | `record TiempoLibre(int horas)` | No negativo |
| `ClausulaDeConsolidacion` | `record ClausulaDeConsolidacion(boolean permitida, List<String> restricciones)` | Lista inmutable, nunca nula. Es lo que viaja en el contrato 1 y sostiene VIA-04 |

Enumeraciones: `TipoDeCarga` (`PALETIZADA` · `GENERAL` · `MAQUINARIA_PESADA`), `TipoDeUnidad`
(`FURGON` · `PLATAFORMA` · `CAMA_BAJA`), `TipoDeRecargo` (`COMBUSTIBLE` · `PELIGROSIDAD` · `NOCTURNIDAD` ·
`ZONA_DIFICIL`), `ModalidadDePago` (`CONTADO` · `CREDITO`), `SituacionCrediticia` (`VIGENTE` · `SUSPENDIDO`),
`EstadoDeCotizacion` (`EMITIDA` · `ACEPTADA` · `RECHAZADA` · `VENCIDA`), `EstadoDeOrden`
(`BORRADOR` · `CONFIRMADA` · `PROGRAMADA` · `DESPACHADA` · `CANCELADA`), `MotivoDeRechazo`
(`PRECIO` · `PLAZO` · `CAPACIDAD` · `OTRO`).

`TipoDeCarga`, `TipoDeUnidad` y `SituacionCrediticia` se duplican respecto de otros contextos a propósito:
no hay módulo común y no se va a crear.

**`Tarifa.total()` — el orden importa y se prueba con importes exactos:**

```
subtotal = base + Σ (base × recargo.porcentaje / 100)
total    = subtotal − (subtotal × descuento.porcentaje / 100)
```

Primero todos los recargos sobre la base, después el descuento sobre el subtotal. Con base 1000,
recargo de 10 % y descuento de 10 %: subtotal 1100.00, total 990.00 — **no** 1000.00. Aplicar el descuento
antes daría 990.00 también en este caso simétrico, así que la prueba usa recargo 10 % y descuento 15 %:
subtotal 1100.00, total 935.00.

### Agregados — `models/entity`

#### `Cliente`

Campos: `id`, `Ruc`, `RazonSocial`, `CondicionDePago condicionHabitual`, `EstadoCrediticio`.

| Método | Contrato |
|---|---|
| `puedeContratarACredito()` | **CLI-01**: `false` si el estado crediticio es `SUSPENDIDO` |
| `puedeContratarAlContado()` | Siempre `true`. Es la otra mitad de CLI-01 y se prueba: suspender no deja al cliente fuera del negocio |
| `refrescarEstadoCrediticio(EstadoCrediticio)` | Sustituye la copia local. Rechaza una lectura más antigua que la vigente |

#### `Cotizacion`

Campos: `id`, `clienteId`, `tarifarioId`, `Carga`, `Ruta`, `Tarifa`, `PeriodoDeVigencia`,
`EstadoDeCotizacion`, `MotivoDeRechazo`.

Vigencia de **siete días calendario** desde la emisión: `PeriodoDeVigencia.diasDeVigencia() == 7` se valida
en la fábrica `emitir`.

| Método | Contrato |
|---|---|
| `haVencidoEn(LocalDate)` | Fuera del periodo de vigencia |
| `aceptar(LocalDate)` | **COT-01**: una cotización vencida lanza `CotizacionVencidaException`; sólo cabe recotizar. Aceptar una ya `ACEPTADA` o `RECHAZADA` también lanza |
| `rechazar(MotivoDeRechazo, LocalDate)` | Motivo obligatorio |

**COT-02** vive en `Descuento`: un descuento sin `autorizadoPor` no se puede construir, así que una tarifa
por debajo del tarifario sin autorización de gerencia es inexpresable.

#### `OrdenDeServicio`

Campos: `id`, `clienteId`, `contratoId` (opcional, escalar), `Carga`, `Ruta`, `Tarifa`, `CondicionDePago`,
`EstadoDeOrden`, `Tarifa falsoFlete` (nulo hasta que se cancela tras el despacho).

| Método | Contrato |
|---|---|
| `crear(...)` (fábrica) | **ORD-02**: recibe el `EstadoCrediticio` **vigente y obligatorio**; si la condición es `CREDITO` y el estado es `SUSPENDIDO`, lanza `CondicionDePagoInconsistenteException`. El estado no es opcional (D2): una elegibilidad no verificable no se trata como favorable |
| `confirmar()` | De `BORRADOR` a `CONFIRMADA`. Otra transición lanza |
| `marcarProgramada()` · `marcarDespachada()` | Transiciones que usa el resto del flujo |
| `reajustarCarga(Carga, Dinero importeDelReajuste)` | **ORD-01**: en `BORRADOR` o `CONFIRMADA` cambia la carga sin más; en `PROGRAMADA` o `DESPACHADA` **exige** el importe del reajuste y lo añade como recargo. Sin importe lanza `ReajusteRequeridoException` |
| `cancelar(LocalDate, String autorizadoPor)` | Antes del despacho, cancela. **Después del despacho genera falso flete por la mitad de la tarifa** y exige `autorizadoPor` no vacío |

#### `ContratoMarco` — entidad hija `TarifaPactada`

Campos: `id`, `clienteId`, `PeriodoDeVigencia`, `TiempoLibre`, `ClausulaDeConsolidacion`,
`List<TarifaPactada>`. Vigencia de un año.

| Método | Contrato |
|---|---|
| `tarifaPara(Ruta, TipoDeUnidad, LocalDate)` | **CTM-01**: devuelve `Optional.empty()` fuera del periodo de vigencia, aunque exista la tarifa pactada |
| `obligaAConsolidar()` | **CTM-02**: la cláusula aplica a **todas** las órdenes del contrato, no orden por orden |
| `admiteConsolidacionDe(Ruta)` | Cruza `permitida` con `restricciones` |

`TarifaPactada` — entidad hija: `id`, `Ruta`, `TipoDeUnidad`, `Dinero precio`.

#### `Tarifario`

Campos: `id`, `PeriodoDeVigencia`, `List<PrecioDeTarifario> precios`, `List<Recargo> recargosEstandar`.

| Método | Contrato |
|---|---|
| `tarifaPara(Ruta, TipoDeUnidad)` | `Optional<Dinero>` |
| `estaVigenteEn(LocalDate)` | — |
| `sucedeA(Tarifario anterior)` | **TAR-01**: si los periodos se solapan, lanza `TarifarioVigenteDuplicadoException`. Es la parte de TAR-01 que el dominio puede sostener; la unicidad global la cierra un índice único más la comprobación del repositorio en `S2`, y así queda anotado |

`PrecioDeTarifario` — entidad hija: `id`, `Ruta`, `TipoDeUnidad`, `Dinero precio`.

### Excepciones — `exceptions`

Raíz `DominioComercialException`; herederas `RucInvalidoException`, `MonedaIncompatibleException`,
`CotizacionVencidaException`, `DescuentoNoAutorizadoException`, `CondicionDePagoInconsistenteException`,
`ReajusteRequeridoException`, `TarifarioVigenteDuplicadoException`, `TransicionDeOrdenInvalidaException`.

### Pruebas exigidas por este slice

| Invariante | Prueba mínima |
|---|---|
| **CLI-01** | Cliente `SUSPENDIDO`: `puedeContratarACredito()` es `false` **y** `puedeContratarAlContado()` es `true`. Las dos mitades, no sólo la prohibición |
| **COT-01** | Aceptar una cotización vencida lanza; en el último día de vigencia **no** lanza (borde exacto, regla D5) |
| **COT-02** | `Descuento` con `autorizadoPor` vacío o nulo lanza; con 4 % y con 16 % lanza; con 5 % y con 15 % no lanza |
| **ORD-01** | `reajustarCarga` sobre una orden `PROGRAMADA` sin importe de reajuste lanza; con importe, lo añade a la tarifa. En `BORRADOR` no exige importe |
| **ORD-02** | Orden a `CREDITO` para un cliente `SUSPENDIDO` lanza; la misma orden al `CONTADO` se crea. El `EstadoCrediticio` nulo lanza, no se asume favorable |
| **CTM-01** | `tarifaPara` devuelve vacío un día después del fin de vigencia y valor el último día |
| **CTM-02** | Con la cláusula `permitida = false`, `obligaAConsolidar()` es `false` para **todas** las órdenes; no hay excepción por orden |
| **TAR-01** | Dos tarifarios con vigencias solapadas: `sucedeA` lanza. Con vigencias consecutivas, no lanza |

Bordes obligatorios:

- `Tarifa.total()` con base 1000, recargo 10 % y descuento 15 % da exactamente 935.00. Con recargos vacíos
  y sin descuento, 1000.00.
- `Ruc` en los cuatro prefijos válidos y en uno inválido; con diez y con doce dígitos.
- Cancelar tras el despacho: el falso flete es exactamente la mitad de la tarifa, y sin `autorizadoPor` lanza.
- `CondicionDePago` `CREDITO` con plazo 0 lanza; `CONTADO` con plazo 30 lanza.
- Cotización con vigencia de 6 u 8 días lanza en la fábrica.
- Toda operación con fecha nula lanza `IllegalArgumentException`.

### Correcciones tras la revisión de `S1-dominio`

**`admiteConsolidacionDe` inventaba un protocolo de texto.** Buscaba los prefijos `NO_` y `EXCLUYE_` dentro
de cada restricción con `contains`. Eso es una regla de negocio codificada en cadenas de texto libre, que
no aparece en ningún contrato y que se rompe con un espacio o un acento.

Se fija la semántica que faltaba en esta spec, y que era el hueco que el agente rellenó por su cuenta:
**cada restricción de `ClausulaDeConsolidacion` nombra un corredor excluido**, y la comparación es de
igualdad sin distinguir mayúsculas. Es lo que viaja en el contrato 1.

Se aceptan las demás decisiones del agente:

- `TipoDeRecargo.REAJUSTE` y `SOBRECAPACIDAD`, que la spec no enumeraba. El reajuste de ORD-01 se modela
  como recargo sobre la tarifa, que es coherente con `Tarifa.total()`.
- `Cotizacion.emitir` exigiendo exactamente siete días de vigencia, con el borde probado en el día siete.
- `OrdenDeServicio.cancelar` con `Dinero.mitad()`: el falso flete es exactamente la mitad, con escala 2.

Los tres `x != null &&` que quedan no son evasiones: dos normalizan un campo opcional y el tercero es la
rama de `BORRADOR`/`CONFIRMADA`, donde el reajuste no se exige. En `PROGRAMADA` y `DESPACHADA` sí lanza.

### Notas de `S2-persistencia`

**Los recargos de una `Tarifa` no son una `@ElementCollection`.** `Tarifa` se embebe **tres veces** en
este contexto —en la cotización, en la tarifa de la orden y en su falso flete— y Hibernate **no permite
redirigir la tabla de una colección declarada dentro de un `@Embeddable`**: el `@AssociationOverride` con
`joinTable` se ignora en silencio y las tres caen en la misma tabla por defecto `tarifa_recargos`. El gate
lo cazó con `missing table [tarifa_recargos]`.

Los recargos se guardan serializados en una columna con `RecargosConverter`, en formato legible
(`COMBUSTIBLE:10.00;PELIGROSIDAD:5.00`). No se pierde nada: son parte del valor de la tarifa y nunca se
consultan por separado.

**El convertidor propaga el nulo a propósito.** Hibernate decide que un embebido es nulo cuando **todas**
sus columnas lo son. Devolviendo cadena vacía para un `NULL`, un falso flete inexistente se materializaba
como una `Tarifa` vacía en vez de como `null`, y la prueba lo pilló: `expected: null but was: Tarifa@747e`.

`Tarifario.recargosEstandar` sí es una `@ElementCollection`, porque está en la entidad y se usa una vez.

## Slice `S3-api-publica` — decisiones de diseño

La receta común está en [§8 del método de trabajo](../README.md#8-receta-de-s3-api-publica) y el módulo
de referencia, ya terminado, es **`msvc-conductores`**. Se copia su forma exacta: `RelojConfig`,
`RecursoNoEncontradoException`, `ConflictoDeRecursoException`, `ManejadorDeErrores`, servicios de
aplicación concretos, DTO `record`, mapeadores estáticos de una sola dirección.

Aquí sólo va lo propio de este contexto.

### Mapa de excepciones a códigos HTTP

`DominioComercialException` queda de comodín en `422`. Las demás se listan una a una, y Spring elige siempre
la más específica.

| Excepción | Código | Por qué |
|---|---:|---|
| `RucInvalidoException` | `400` | El objeto de valor rechaza el formato del RUC. Es entrada mal formada, no invariante. |
| `CotizacionVencidaException` | `409` | COT-01. La cotización vencida no se acepta *ahora*; el camino abierto es recotizar. |
| `TransicionDeOrdenInvalidaException` | `409` | La orden no está en el estado que la operación exige. |
| `TarifarioVigenteDuplicadoException` | `409` | TAR-01. Ya hay uno vigente: es un choque de estado, no del cuerpo. |
| `ReajusteRequeridoException` | `409` | ORD-01. La orden ya está programada; con la orden sin programar el mismo cuerpo valdría. |
| `DescuentoNoAutorizadoException` | `422` | COT-02. Al cuerpo le falta la autorización de gerencia y seguirá faltándole. |
| `CondicionDePagoInconsistenteException` | `422` | ORD-02. La condición pedida no cuadra con el estado crediticio recibido. |
| `MonedaIncompatibleException` | `422` | Dos importes de distinta moneda en la misma operación. |

Además, en todos los módulos: `RecursoNoEncontradoException` → `404`, `ConflictoDeRecursoException` →
`409`, `IllegalArgumentException` → `400`, y la validación de forma → `400` con el detalle campo a campo
bajo la clave `errores`.

### Servicios de aplicación

Uno por raíz de agregado, con el nombre del agregado: ClienteService, CotizacionService, OrdenDeServicioService, ContratoMarcoService, TarifarioService.
Ninguno decide reglas: cargan, llaman al método del agregado y guardan. Las dos únicas comprobaciones
admitidas son la existencia (`404`) y la unicidad contra el repositorio (`409`).

### El `404` que las tablas de arriba no escriben

Toda ruta con `{id}` puede devolver `404`, se diga o no en la columna de códigos: pedir un subrecurso
de un agregado que no existe no es un `400`. Las tablas de la API de este documento se escribieron en
`S1` y omiten ese caso; el código no lo omite.
