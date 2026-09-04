# msvc-unidades — Gestión de Unidades

| | |
|---|---|
| Bounded context | Gestión de Unidades |
| Subdominio | Support |
| Puerto | `8040` |
| Esquema | `mirador_unidades` |
| Paquete raíz | `pe.edu.unc.elmirador.unidades` |
| Responsable de revisión | Arnold Ocas |
| Agregados | 3 |
| Invariantes | 6 (UNI-01/02/03, OMT-01/02, REP-01) |

## Responsabilidad

Mantener la hoja de vida de las catorce unidades de la flota, programar el mantenimiento preventivo por
kilometraje, controlar la vigencia de los cuatro documentos obligatorios y administrar el stock de repuestos
críticos.

Su salida hacia el Core es una sola: **decir si una unidad puede o no ser asignada a un viaje**.

Está en relación `Partnership` con `msvc-conductores`: ambos los administra el encargado de flota y
evolucionan de forma coordinada.

## Agregados

### `Unidad` — raíz `Unidad`, entidad hija `DocumentoVehicular`

- **Objetos de valor**: `Placa`, `TipoDeUnidad`, `Capacidad`, `Kilometraje`, `EstadoOperativo`,
  `ProgramaDeMantenimiento`, `TipoDeDocumento`, `PeriodoDeVigencia`
- **Métodos**: `actualizarKilometraje(km)`, `estáHabilitada()`, `marcarInoperativa(motivo)`
- **Invariantes**: UNI-01, UNI-02, UNI-03

`TipoDeUnidad` ∈ `FURGON` · `PLATAFORMA` · `CAMA_BAJA`, con `admite(carga)` y `licenciaRequerida()`.
La maquinaria pesada exige cama baja; la carga paletizada va en furgón o plataforma.

`EstadoOperativo` ∈ `OPERATIVA` · `EN_TALLER` · `INOPERATIVA`, con motivo. Sólo `OPERATIVA` es asignable.

`DocumentoVehicular` cubre los cuatro documentos obligatorios: revisión técnica, SOAT, permiso de operación
del MTC y certificado de habilitación vehicular. Con `estáVigente()` en falso para **cualquiera** de ellos,
la unidad pasa automáticamente a `INOPERATIVA` (UNI-01). No es una alerta: es un cambio de estado.

`ProgramaDeMantenimiento` guarda `kmÚltimoServicio`, `kmPróximoServicio` e `intervalo`, con
`estáVencido(km)` y `requiereAlerta(km)`. La alerta se dispara a 500 km del próximo servicio.
Intervalos del negocio: 10 000 km (aceite y filtros), 20 000 km (revisión mayor), 40 000 km (llantas).

`Kilometraje` impide registrar un valor menor al vigente (UNI-03).

### `OrdenDeMantenimiento` — raíz `OrdenDeMantenimiento`, entidad hija `TrabajoRealizado`

- **Objetos de valor**: `TipoDeMantenimiento`, `Kilometraje`, `Dinero`
- **Referencias**: `UnidadId`, `RepuestoId`
- **Método**: `cerrar()`
- **Invariantes**: OMT-01, OMT-02

`TipoDeMantenimiento` ∈ `PREVENTIVO` · `CORRECTIVO`.

### `Repuesto` — raíz `Repuesto`

- **Objetos de valor**: `Dinero`
- **Métodos**: `ajustarInventario(cantidad)`, `requiereReposicion()`
- **Invariante**: REP-01

## API pública `/api/v1`

| Método | Ruta | Qué hace | Códigos |
|---|---|---|---|
| `POST` | `/unidades` | Registra una unidad | `201` `400` `409` |
| `GET` | `/unidades/{id}` | Consulta la hoja de vida | `200` `404` |
| `GET` | `/unidades` | Lista con filtro por estado operativo | `200` |
| `POST` | `/unidades/{id}/documentos` | Registra o renueva un documento | `201` `400` |
| `POST` | `/unidades/{id}/estado` | Cambia el estado operativo con motivo | `200` `409` |
| `POST` | `/ordenes-mantenimiento` | Abre una orden de taller | `201` `422` (OMT-02) |
| `POST` | `/ordenes-mantenimiento/{id}/trabajos` | Registra un trabajo | `201` `409` (OMT-01) |
| `POST` | `/ordenes-mantenimiento/{id}/cerrar` | Cierra la orden y actualiza el programa | `200` `409` |
| `POST` | `/repuestos` | Registra un repuesto con su stock mínimo | `201` `400` |
| `POST` | `/repuestos/{id}/movimientos` | Ajusta el inventario | `200` `422` (REP-01) |
| `GET` | `/alertas` | Documentos por vencer y mantenimientos próximos | `200` |

## API interna `/internal/v1`

Publica los contratos **2** y **5**.

| Método | Ruta | Consumidor | Contrato |
|---|---|---|---|
| `GET` | `/unidades/{unidadId}/elegibilidad` | Programación | 2 |
| `POST` | `/unidades/{unidadId}/kilometraje` | Ejecución | 5 |
| `POST` | `/unidades/{unidadId}/fallas` | Ejecución | 5 |

El endpoint de elegibilidad concentra UNI-01, UNI-02, la capacidad y la compatibilidad de tipo. Devuelve
`200` con `elegible: false` y la lista de motivos; **no** devuelve error.

## Clientes Feign que consume

Ninguno. Unidades es un proveedor puro.

## Criterios de éxito

- [ ] `./mvnw -pl msvc-unidades verify` en verde (exige Docker: levanta MySQL con Testcontainers)
- [ ] Cada tabla del contexto creada por una migración Flyway; `ddl-auto=validate` en verde
- [ ] `PersistenciaUnidadesIT` en verde contra MySQL real
- [ ] Las 6 invariantes con prueba que las viola
- [ ] Prueba de UNI-01 para los cuatro tipos de documento, uno por uno
- [ ] Prueba de que `actualizarKilometraje()` rechaza un retroceso (UNI-03)
- [ ] `ProgramaDeMantenimiento.requiereAlerta()` probado en el borde de los 500 km
- [ ] `GET /internal/v1/unidades/{id}/elegibilidad` devuelve `200` con `elegible:false` y motivo, no un `4xx`
- [ ] `POST .../kilometraje` devuelve `409` ante un retroceso, e idempotencia probada
- [ ] `POST .../fallas` con `dejaInoperativa:true` deja la unidad no elegible en la consulta siguiente
- [ ] 0 imports de otro contexto
- [ ] Sano en `./scripts/smoke-test.sh`

---

## Slice `S1-dominio` — decisiones de diseño

Este slice entrega **sólo el modelo de dominio y sus pruebas**. Sin repositorios, sin controladores, sin
`@Entity` y sin migraciones: eso es `S2`. Los objetos de valor sí llevan ya `@Embeddable`, porque la anotación
es inerte mientras ninguna entidad los referencie y evita reescribirlos en `S2`.

### Correspondencia con el diseño táctico (regla 13)

| Diseño táctico | Código |
|---|---|
| `estáHabilitada()` | `estaHabilitada()` |
| `estáVigente()` | `estaVigente()` |
| `estáVencido(km)` | `estaVencido(km)` |
| `kmÚltimoServicio` | `kmUltimoServicio` |
| `kmPróximoServicio` | `kmProximoServicio` |
| `requiereReposición()` | `requiereReposicion()` |

### Objetos de valor — `models/vo`

Todos `record` inmutables, anotados `@Embeddable`, validando en el constructor compacto.

| Tipo | Forma | Comportamiento |
|---|---|---|
| `Placa` | `record Placa(String valor)` | Normaliza a mayúsculas. Formato peruano `AAA-000` o `A0A-000`; otro valor lanza `IllegalArgumentException` |
| `Capacidad` | `record Capacidad(int pesoMaximoKg, BigDecimal volumenMaximoM3)` | `admite(int pesoKg, BigDecimal volumenM3)`. Ambos deben ser positivos |
| `Kilometraje` | `record Kilometraje(int valor)` | No negativo. `avanzarA(Kilometraje nuevo)` devuelve el nuevo o lanza `KilometrajeRetrocedeException` (**UNI-03**) |
| `PeriodoDeVigencia` | `record PeriodoDeVigencia(LocalDate desde, LocalDate hasta)` | `hasta` posterior a `desde`. `estaVigenteEn(LocalDate)`, `venceDentroDe(int dias, LocalDate ref)` |
| `ProgramaDeMantenimiento` | `record ProgramaDeMantenimiento(Kilometraje kmUltimoServicio, Kilometraje kmProximoServicio, IntervaloDeMantenimiento intervalo)` | `estaVencido(Kilometraje km)`, `requiereAlerta(Kilometraje km)` a 500 km o menos del próximo servicio |
| `EstadoOperativo` | `record EstadoOperativo(SituacionOperativa situacion, String motivo)` | `esAsignable()` ⇔ `situacion == OPERATIVA`. Fábricas `operativa()`, `enTaller(motivo)`, `inoperativa(motivo)`. Toda situación distinta de `OPERATIVA` exige motivo no vacío |
| `Dinero` | `record Dinero(BigDecimal monto, String codigoMoneda)` | Monto no negativo con escala 2, moneda ISO-4217 de 3 letras. `sumar`, `multiplicarPor(int)`; operar con monedas distintas lanza `MonedaIncompatibleException` |

Enumeraciones, también en `models/vo`:

| Enum | Valores | Comportamiento |
|---|---|---|
| `TipoDeUnidad` | `FURGON` · `PLATAFORMA` · `CAMA_BAJA` | `admite(TipoDeCarga)`, `licenciaRequerida()` |
| `TipoDeCarga` | `PALETIZADA` · `GENERAL` · `MAQUINARIA_PESADA` | — |
| `CategoriaDeLicencia` | `A_IIIA` · `A_IIIB` · `A_IIIC` | — |
| `TipoDeDocumento` | `REVISION_TECNICA` · `SOAT` · `PERMISO_MTC` · `HABILITACION_VEHICULAR` | — |
| `SituacionOperativa` | `OPERATIVA` · `EN_TALLER` · `INOPERATIVA` | — |
| `IntervaloDeMantenimiento` | `ACEITE_Y_FILTROS` (10 000 km) · `REVISION_MAYOR` (20 000) · `LLANTAS` (40 000) | `kilometros()` |
| `TipoDeMantenimiento` | `PREVENTIVO` · `CORRECTIVO` | — |
| `EstadoDeOrden` | `ABIERTA` · `CERRADA` | — |
| `MotivoDeNoElegibilidad` | `DOCUMENTO_VENCIDO` · `MANTENIMIENTO_VENCIDO` · `EN_TALLER` · `INOPERATIVA` · `CAPACIDAD_INSUFICIENTE` · `TIPO_INCOMPATIBLE` | `codigo()` y `codigo(String detalle)` → `"DOCUMENTO_VENCIDO:SOAT"` |

**Tablas de decisión.** `admite` y `licenciaRequerida` son la fuente de verdad del negocio:

| `TipoDeUnidad` | `PALETIZADA` | `GENERAL` | `MAQUINARIA_PESADA` | `licenciaRequerida()` |
|---|:---:|:---:|:---:|---|
| `FURGON` | sí | sí | **no** | `A_IIIA` |
| `PLATAFORMA` | sí | sí | **no** | `A_IIIB` |
| `CAMA_BAJA` | **no** | sí | sí | `A_IIIC` |

`CategoriaDeLicencia` se duplica aquí y en `msvc-conductores` a propósito: son contextos distintos y no
comparten código. La coincidencia se verifica en el contrato 3, nunca por un import.

### Agregados — `models/entity`

Clases ricas, **sin anotaciones JPA en este slice**. Identidad con `String` (`UnidadId` no es un VO: el
identificador de la raíz se modela como campo `String id`, y las referencias a otros agregados también).

`Unidad` — raíz. Campos: `id`, `Placa`, `TipoDeUnidad`, `Capacidad`, `Kilometraje`, `EstadoOperativo`,
`ProgramaDeMantenimiento`, `List<DocumentoVehicular> documentos`.

| Método | Contrato |
|---|---|
| `actualizarKilometraje(Kilometraje nuevo)` | Delega en `Kilometraje.avanzarA`. Propaga el fallo (**UNI-03**) |
| `registrarDocumento(TipoDeDocumento, PeriodoDeVigencia)` | Reemplaza el documento del mismo tipo. Al terminar, reevalúa el estado |
| `evaluarVigenciaDocumental(LocalDate)` | Si **cualquier** documento falta o no está vigente, pasa a `INOPERATIVA` con motivo `DOCUMENTO_VENCIDO:<tipo>` (**UNI-01**). Es un cambio de estado, no una alerta |
| `estaHabilitada(LocalDate)` | `false` si el estado no es asignable, si falta o venció un documento, o si el mantenimiento preventivo está vencido (**UNI-02**) |
| `marcarInoperativa(String motivo)` | Fuerza `INOPERATIVA`. Motivo obligatorio |
| `motivosDeNoElegibilidad(LocalDate, int pesoKg, BigDecimal volumenM3, TipoDeCarga)` | Devuelve la lista de motivos del contrato 2, vacía si es elegible. Alimenta `S4` sin que el controlador decida nada |

`DocumentoVehicular` — entidad hija: `id`, `TipoDeDocumento`, `PeriodoDeVigencia`, `numero`. `estaVigente(LocalDate)`.

`OrdenDeMantenimiento` — raíz. Campos: `id`, `unidadId`, `TipoDeMantenimiento`, `Kilometraje kmAtencion`,
`EstadoDeOrden`, `List<TrabajoRealizado> trabajos`, `fechaApertura`, `fechaCierre`.

| Método | Contrato |
|---|---|
| `abrir(...)` (fábrica) | Rechaza `kmAtencion` menor al del último mantenimiento de la unidad (**OMT-02**) → `KilometrajeDeAtencionInvalidoException` |
| `registrarTrabajo(TrabajoRealizado)` | `OrdenCerradaException` si ya está `CERRADA` (**OMT-01**) |
| `cerrar(LocalDate)` | Idempotencia no: cerrar una orden `CERRADA` lanza `OrdenCerradaException` |
| `costoTotal()` | Suma los `Dinero` de sus trabajos. Con la lista vacía, cero en la moneda de la orden |

`TrabajoRealizado` — entidad hija: `id`, `descripcion`, `Dinero costoManoDeObra`, `repuestoId`, `cantidad`.

`Repuesto` — raíz. Campos: `id`, `codigo`, `descripcion`, `int existencias`, `int stockMinimo`, `Dinero costoUnitario`.

| Método | Contrato |
|---|---|
| `ajustarInventario(int cantidad)` | Positivo entra, negativo sale. Si el resultado es negativo lanza `ExistenciasNegativasException` (**REP-01**) |
| `requiereReposicion()` | `existencias <= stockMinimo` |

### Excepciones — `exceptions`

Todas extienden `RuntimeException` y una raíz común `DominioUnidadesException`:
`KilometrajeRetrocedeException`, `KilometrajeDeAtencionInvalidoException`, `OrdenCerradaException`,
`ExistenciasNegativasException`, `MonedaIncompatibleException`, `PlacaInvalidaException`.

Traducirlas a `application/problem+json` es trabajo de `S3`; aquí sólo se lanzan.

### Pruebas exigidas por este slice

JUnit 5 puro, sin `@SpringBootTest`. Una clase por agregado o VO con lógica, en
`src/test/java/pe/edu/unc/elmirador/unidades/models/`.

| Invariante | Prueba mínima |
|---|---|
| **UNI-01** | Cuatro casos, uno por `TipoDeDocumento`: con ese documento vencido y los otros tres vigentes, la unidad queda `INOPERATIVA` con motivo `DOCUMENTO_VENCIDO:<tipo>`. Más un caso con el documento ausente |
| **UNI-02** | Unidad `OPERATIVA`, documentos vigentes y `ProgramaDeMantenimiento` vencido ⇒ `estaHabilitada()` es `false` |
| **UNI-03** | `actualizarKilometraje` con un valor menor lanza; con el mismo valor **no** lanza (no decrece) |
| **OMT-01** | `registrarTrabajo` y `cerrar` sobre una orden `CERRADA` lanzan |
| **OMT-02** | `abrir` con `kmAtencion` menor al último mantenimiento lanza; igual no lanza |
| **REP-01** | `ajustarInventario(-n)` que deja negativo lanza y **no** altera las existencias; dejar exactamente cero no lanza |

Bordes obligatorios, además de las invariantes:

- `requiereAlerta` en 501, 500 y 499 km del próximo servicio.
- `TipoDeUnidad.admite` para las 9 combinaciones de la tabla, y `licenciaRequerida` para los 3 tipos.
- `Capacidad.admite` con el peso exacto y con el volumen exacto del máximo.
- `motivosDeNoElegibilidad` acumulando dos motivos a la vez, y devolviendo lista vacía en el caso elegible.
- `Dinero` sumando monedas distintas lanza.
