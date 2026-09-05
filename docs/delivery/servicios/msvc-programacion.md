# msvc-programacion — Programación y Despacho

| | |
|---|---|
| Bounded context | Programación y Despacho |
| Subdominio | **Core Domain** |
| Puerto | `8020` |
| Esquema | `mirador_programacion` |
| Paquete raíz | `pe.edu.unc.elmirador.programacion` |
| Responsable de revisión | Brayam Alfaro |
| Agregados | 3 |
| Invariantes | 11 (VIA-01…07, AGU-01/02, AGC-01/02) |

## Responsabilidad

Es el Core Domain. Decide qué órdenes se consolidan en un mismo viaje, qué unidad y qué conductor lo
ejecutan, en qué orden se recorren las paradas, y emite la hoja de ruta.

Es la actividad que más valor agrega y la que más diferencia a la empresa: optimiza el uso de la flota y
reduce el retorno vacío.

> **Este servicio concentra el «20 % duro».** La consolidación (VIA-02…VIA-05) y la secuencia de estiba
> (VIA-06) las implementa Claude, no el ejecutor. El resto del módulo sí se delega.

## Agregados

### `Viaje` — raíz `Viaje`

- **Objetos de valor**: `Ruta`, `VentanaDeTiempo`, `CargaConsolidada`, `AsignaciónDeRecursos`,
  `EstadoDeViaje`, `HojaDeRuta` (que contiene las `Parada` planificadas)
- **Referencias**: `OrdenDeServicioId` (varias), `UnidadId`, `ConductorId`
- **Métodos**: `consolidarOrden(orden)`, `asignarRecursos(unidad, conductores)`, `confirmarProgramacion()`,
  `autorizarDespacho()`
- **Invariantes**: VIA-01 … VIA-07

`EstadoDeViaje` ∈ `PLANIFICADO` · `PROGRAMADO` · `DESPACHADO` · `CANCELADO`, y valida las transiciones
permitidas. No se salta estados.

Métodos de los objetos de valor, con el nombre exacto del diseño táctico:

| Objeto de valor | Método | Qué resuelve |
|---|---|---|
| `CargaConsolidada` | `pesoTotal()`, `cabeEn(capacidad)` | VIA-02 |
| `Ruta` | `mismoCorredorQue(otra)` | VIA-03 |
| `VentanaDeTiempo` | `seSolapaCon(otra)` | VIA-03, AGU-01, AGC-01 |
| `Carga` | `esCompatibleCon(otra)` | VIA-05 |
| `HojaDeRuta` | `secuenciaDeEstiba()` | VIA-06 |
| `AsignaciónDeRecursos` | `esCompleta()` | VIA-01 |

`AsignaciónDeRecursos` admite un segundo conductor **únicamente** en viajes con relevo (caso Lima, donde el
trayecto supera las horas de conducción continua permitidas).

`Parada` es inmutable: si el itinerario cambia, no se modifica la parada — se emite una hoja de ruta nueva.

### `AgendaDeUnidad` — raíz `AgendaDeUnidad`, entidad hija `ReservaDeUnidad`

- **Identidad**: la unidad misma (`UnidadId`)
- **Objetos de valor**: `VentanaDeTiempo`, `EstadoDeReserva`
- **Métodos**: `reservar(ventana)`, `confirmar(reserva)`, `liberar(reserva)`
- **Invariantes**: AGU-01, AGU-02

`EstadoDeReserva` ∈ `TENTATIVA` · `CONFIRMADA` · `LIBERADA`. Sólo las dos primeras bloquean el recurso.

### `AgendaDeConductor` — raíz `AgendaDeConductor`, entidad hija `ReservaDeConductor`

- **Identidad**: el conductor mismo (`ConductorId`)
- Mismos objetos de valor, métodos e invariantes que la agenda de unidad, aplicados al recurso humano
- **Invariantes**: AGC-01, AGC-02

## API pública `/api/v1`

| Método | Ruta | Qué hace | Códigos |
|---|---|---|---|
| `POST` | `/viajes` | Crea un viaje en `PLANIFICADO` a partir de una orden confirmada | `201` `404` `409` |
| `POST` | `/viajes/{id}/ordenes` | Consolida otra orden en el viaje | `200` `409` (VIA-02…05, VIA-07) |
| `POST` | `/viajes/{id}/recursos` | Asigna unidad y conductores verificando elegibilidad | `200` `409` `503` |
| `POST` | `/viajes/{id}/programar` | Pasa a `PROGRAMADO` y emite la hoja de ruta | `200` `409` (VIA-01) |
| `POST` | `/viajes/{id}/despachar` | Pasa a `DESPACHADO` | `200` `409` |
| `POST` | `/viajes/{id}/cancelar` | Cancela y libera las reservas | `200` `409` |
| `GET` | `/viajes/{id}` | Consulta el viaje | `200` `404` |
| `GET` | `/agendas/unidades/{unidadId}` | Consulta la agenda de una unidad | `200` `404` |
| `GET` | `/agendas/conductores/{conductorId}` | Consulta la agenda de un conductor | `200` `404` |

## API interna `/internal/v1`

Publica el contrato **4**.

| Método | Ruta | Consumidor | Contrato |
|---|---|---|---|
| `GET` | `/viajes/{viajeId}/hoja-de-ruta` | Ejecución | 4 |

## Clientes Feign que consume

| Cliente | Servicio | Contrato | Propiedad |
|---|---|---|---|
| `ComercialClient` | Comercial | 1 | `clients.comercial.url` |
| `UnidadesClient` | Unidades | 2 | `clients.unidades.url` |
| `ConductoresClient` | Conductores | 3 | `clients.conductores.url` |

`POST /viajes/{id}/recursos` no asigna nada si Unidades o Conductores no responden: devuelve `503`. Una
elegibilidad no verificable **no** se trata como elegible.

## Criterios de éxito

- [ ] `./mvnw -pl msvc-programacion verify` en verde (exige Docker: levanta MySQL con Testcontainers)
- [ ] Cada tabla del contexto creada por una migración Flyway; `ddl-auto=validate` en verde
- [ ] `PersistenciaProgramacionIT` en verde contra MySQL real
- [ ] Las 11 invariantes con prueba que las viola
- [ ] `CargaConsolidada.cabeEn()` probado en el límite exacto de capacidad, no sólo por encima y por debajo
- [ ] `VentanaDeTiempo.seSolapaCon()` probado con solape parcial, contención total y bordes que se tocan
- [ ] `HojaDeRuta.secuenciaDeEstiba()` devuelve el orden inverso de descarga, con prueba de tres paradas
- [ ] Transiciones de `EstadoDeViaje` probadas, incluidas las prohibidas
- [ ] Los 3 clientes Feign con timeout, traducción de error y prueba con stub que cubre el `503`
- [ ] `GET /internal/v1/viajes/{id}/hoja-de-ruta` devuelve `409` para un viaje en `PLANIFICADO`
- [ ] 0 imports de otro contexto
- [ ] Sano en `./scripts/smoke-test.sh`

---

## Slices `S1a-dominio-base` y `S1b-consolidacion` — decisiones de diseño

Sólo dominio y pruebas. Sin `@Entity`, sin repositorios, sin controladores, sin Feign, sin migraciones.
Rigen las ocho **reglas de dominio** de [`../README.md`](../README.md#6-reglas-de-dominio).

El slice está partido porque el reparto de trabajo lo exige: `S1a` es mecánico y se delega; `S1b` es el
«20 % duro» —consolidación y estiba— y lo implementa Claude. **`S1a` deja los huecos declarados**, con la
firma exacta que `S1b` rellenará, para que las dos mitades encajen sin renegociar nada.

### Correspondencia con el diseño táctico (regla 13)

| Diseño táctico | Código |
|---|---|
| `AsignaciónDeRecursos` | `AsignacionDeRecursos` |
| `secuenciaDeEstiba()` | igual |
| `tramoDeGestión()` | no aplica aquí |
| `Programación` | `Programacion` |

### El tiempo se modela con `OffsetDateTime`

Las ventanas de reserva son instantes, no días. El contrato 2 y el 3 mandan `desde`/`hasta` en ISO 8601 con
offset (regla 6), y la regla D5 ya costó un defecto por usar fechas donde hacían falta instantes. Aquí
`VentanaDeTiempo` usa `OffsetDateTime` desde el principio.

---

## `S1a-dominio-base` — AGU-01, AGU-02, AGC-01, AGC-02, VIA-01, VIA-07

### Objetos de valor — `models/vo`

| Tipo | Forma | Comportamiento |
|---|---|---|
| `VentanaDeTiempo` | `record VentanaDeTiempo(OffsetDateTime desde, OffsetDateTime hasta)` | `hasta` posterior a `desde`. **`seSolapaCon(otra)`: `desde < otra.hasta && otra.desde < hasta`.** Dos ventanas que sólo se tocan en el borde **no** se solapan (regla D5) |
| `Capacidad` | `record Capacidad(int pesoMaximoKg, BigDecimal volumenMaximoM3)` | Ambos positivos |
| `Carga` | `record Carga(String ordenDeServicioId, int pesoKg, BigDecimal volumenM3, TipoDeCarga tipo, int secuenciaDeDescarga)` | Peso y volumen positivos. `esCompatibleCon(otra)` la implementa `S1b` |
| `Ruta` | `record Ruta(String origen, String destino, String corredor)` | `mismoCorredorQue(otra)` la implementa `S1b` |
| `ElegibilidadDeRecurso` | `record ElegibilidadDeRecurso(boolean elegible, List<String> motivos)` | La respuesta de los contratos 2 y 3, tal como llegó. Lista inmutable, nunca nula. `elegible == false` exige motivos no vacíos |
| `AsignacionDeRecursos` | `record AsignacionDeRecursos(String unidadId, List<String> conductorIds, boolean conRelevo)` | `esCompleta()`: unidad no vacía y al menos un conductor. **Un segundo conductor sólo con `conRelevo`**; tres o más lanza siempre |
| `EstadoDeViaje` | enum | `PLANIFICADO` · `PROGRAMADO` · `DESPACHADO` · `CANCELADO`, con `puedeTransicionarA(EstadoDeViaje)` |
| `EstadoDeReserva` | enum | `TENTATIVA` · `CONFIRMADA` · `LIBERADA`. `bloqueaElRecurso()` es `true` en las dos primeras |
| `TipoDeCarga` | enum | `PALETIZADA` · `GENERAL` · `MAQUINARIA_PESADA` |

**Transiciones permitidas de `EstadoDeViaje`**, y ninguna otra:

| Desde | Hacia |
|---|---|
| `PLANIFICADO` | `PROGRAMADO` · `CANCELADO` |
| `PROGRAMADO` | `DESPACHADO` · `CANCELADO` |
| `DESPACHADO` | — (terminal) |
| `CANCELADO` | — (terminal) |

No se salta de `PLANIFICADO` a `DESPACHADO`.

### Agregados `AgendaDeUnidad` y `AgendaDeConductor`

Simétricos. La identidad es el recurso: `unidadId` y `conductorId`.

Campos: el id del recurso y `List<ReservaDeUnidad>` / `List<ReservaDeConductor>`.

| Método | Contrato |
|---|---|
| `reservar(String reservaId, VentanaDeTiempo, ElegibilidadDeRecurso, String viajeId)` | **AGU-01 / AGC-01**: si se solapa con una reserva que bloquea el recurso, lanza `ReservaSolapadaException`. **AGU-02 / AGC-02**: si `elegibilidad.elegible()` es `false`, lanza `RecursoNoElegibleException` con los motivos. La elegibilidad es **obligatoria** (regla D2): no verificarla no equivale a ser elegible |
| `confirmar(String reservaId)` · `liberar(String reservaId)` | Cambian el estado de la reserva. Una liberada no se reconfirma |
| `reservasQueBloquean()` | Las `TENTATIVA` y `CONFIRMADA` |

Una reserva `LIBERADA` **no** bloquea: se puede reservar sobre su ventana.

`ReservaDeUnidad` / `ReservaDeConductor` — entidades hijas: `id`, `viajeId`, `VentanaDeTiempo`, `EstadoDeReserva`.

### Agregado `Viaje` — parte de `S1a`

Campos: `id`, `Ruta`, `VentanaDeTiempo`, `CargaConsolidada`, `AsignacionDeRecursos` (nulo hasta asignar),
`EstadoDeViaje`, `HojaDeRuta` (nula hasta programar), `List<String> ordenIds`.

| Método | Slice | Contrato |
|---|---|---|
| `planificar(...)` (fábrica) | `S1a` | Nace `PLANIFICADO` con la primera orden consolidada |
| `asignarRecursos(AsignacionDeRecursos)` | `S1a` | Sobre un viaje `DESPACHADO` o `CANCELADO` lanza |
| `confirmarProgramacion(HojaDeRuta)` | `S1a` | **VIA-01**: sin `AsignacionDeRecursos.esCompleta()` lanza `AsignacionIncompletaException`. Pasa a `PROGRAMADO` |
| `autorizarDespacho()` | `S1a` | De `PROGRAMADO` a `DESPACHADO` |
| `cancelar()` | `S1a` | Desde `PLANIFICADO` o `PROGRAMADO` |
| `consolidarOrden(...)` | **`S1b`** | **VIA-07** es la primera comprobación y sí es de `S1a`: sobre un viaje `DESPACHADO` lanza `ViajeDespachadoException`. El resto de VIA-02…05 es de `S1b` |

**`S1a` deja `consolidarOrden` con esta firma exacta y sólo la comprobación de VIA-07 implementada:**

```java
public void consolidarOrden(
        Carga carga,
        Ruta rutaDeLaOrden,
        VentanaDeTiempo ventanaDeLaOrden,
        ClausulaDeConsolidacion clausulaDelContrato,
        Capacidad capacidadDeLaUnidad)
```

Y `CargaConsolidada` con estos métodos declarados, devolviendo por ahora lo mínimo para compilar y con la
prueba de `S1b` marcada `@Disabled("S1b")`:

```java
public int pesoTotal()
public BigDecimal volumenTotal()
public boolean cabeEn(Capacidad capacidad)
```

`ClausulaDeConsolidacion` es un `record(boolean permitida, List<String> restricciones)`, copia de lo que
devuelve el contrato 1.

### Pruebas de `S1a`

| Invariante | Prueba mínima |
|---|---|
| **AGU-01** | Dos reservas solapadas de la misma unidad: la segunda lanza. Con la primera `LIBERADA`, no lanza. Ventanas que sólo se tocan en el borde **no** se consideran solapadas |
| **AGU-02** | `reservar` con `elegible = false` lanza y el mensaje incluye los motivos. Con `ElegibilidadDeRecurso` nula lanza: no se asume elegible |
| **AGC-01** | Igual que AGU-01 sobre el conductor |
| **AGC-02** | Igual que AGU-02 sobre el conductor |
| **VIA-01** | `confirmarProgramacion` sin unidad lanza; sin conductores lanza; con ambos pasa a `PROGRAMADO` |
| **VIA-07** | `consolidarOrden` sobre un viaje `DESPACHADO` lanza |

Bordes obligatorios de `S1a`:

- `seSolapaCon` con solape parcial, contención total, bordes que se tocan y disjuntas: los cuatro casos.
- Las transiciones prohibidas de `EstadoDeViaje`, una a una, incluido `PLANIFICADO → DESPACHADO`.
- `AsignacionDeRecursos` con dos conductores y `conRelevo = false` lanza; con `conRelevo = true` no; con tres lanza siempre.
- Toda operación con fecha o ventana nula lanza `IllegalArgumentException`.

---

## `S1b-consolidacion` — VIA-02, VIA-03, VIA-04, VIA-05, VIA-06

**Lo implementa Claude.** Es la regla que más valor agrega del sistema y la que peor tolera una
interpretación libre: cuatro invariantes acopladas que se evalúan en la misma operación.

| Invariante | Dónde vive | Regla |
|---|---|---|
| **VIA-02** | `CargaConsolidada.cabeEn(Capacidad)` | Peso **y** volumen de la carga ya consolidada más la nueva no pueden exceder la capacidad. Se prueba en el límite exacto, no sólo por encima y por debajo |
| **VIA-03** | `Ruta.mismoCorredorQue(otra)` y `VentanaDeTiempo.seSolapaCon(otra)` | Mismo corredor **y** ventanas compatibles. Corredor distinto o ventanas disjuntas: no se consolida |
| **VIA-04** | `ClausulaDeConsolidacion.permitida` | Si el contrato marco de la orden lo prohíbe, no se consolida. La cláusula es **obligatoria** en la firma: sin ella no se asume permitido |
| **VIA-05** | `Carga.esCompatibleCon(otra)` | Compatibilidad física por pares contra **todas** las cargas ya consolidadas, no sólo contra la última |
| **VIA-06** | `HojaDeRuta.secuenciaDeEstiba()` | La carga que se descarga primero se estiba al final: la secuencia de estiba es el **orden inverso** de la secuencia de descarga |

**Tabla de compatibilidad de `Carga.esCompatibleCon`:**

| | `PALETIZADA` | `GENERAL` | `MAQUINARIA_PESADA` |
|---|:---:|:---:|:---:|
| `PALETIZADA` | sí | sí | **no** |
| `GENERAL` | sí | sí | **no** |
| `MAQUINARIA_PESADA` | **no** | **no** | sí |

Simétrica por construcción, y la prueba lo comprueba en las nueve celdas en los dos sentidos.

`consolidarOrden` evalúa en este orden y **no muta nada hasta haber pasado las cinco** (regla D6):
VIA-07 → VIA-04 → VIA-03 → VIA-05 → VIA-02. La capacidad va la última porque es la más cara de calcular.

Pruebas exigidas de `S1b`, además de una por invariante que la viole:

- `cabeEn` en el límite exacto de peso, en el límite exacto de volumen, y excediendo sólo uno de los dos.
- `secuenciaDeEstiba()` con tres paradas: descarga 1-2-3 devuelve estiba 3-2-1.
- Una consolidación que falla no altera la carga consolidada ni la lista de órdenes del viaje.
- Consolidar una tercera carga incompatible con la **primera** —no con la segunda— se rechaza.

### Excepciones — `exceptions`

Raíz `DominioProgramacionException`; herederas `ReservaSolapadaException`, `RecursoNoElegibleException`,
`AsignacionIncompletaException`, `ViajeDespachadoException`, `TransicionDeViajeInvalidaException`,
`ConsolidacionProhibidaException` (VIA-04), `CorredorIncompatibleException` (VIA-03),
`CargaIncompatibleException` (VIA-05), `CapacidadExcedidaException` (VIA-02).

### Revisión de `S1a-dominio-base`

Pasó sin correcciones de código: es el primer slice delegado que llega limpio. Lo verificado:

- `VentanaDeTiempo.seSolapaCon` es estrictamente `desde.isBefore(otra.hasta) && otra.desde.isBefore(hasta)`.
  Dos ventanas que se tocan en el borde no se solapan, que era el riesgo de la regla D5 aquí.
- La firma de `Viaje.consolidarOrden` coincide con la spec al parámetro, así que `S1b` encaja sin renegociar.
- Los ocho `// TODO S1b` están donde debían y ninguna prueba de VIA-02…06 se coló.
- Cero `now()`, cero JPA, cero imports cruzados.

Los dos `x != null &&` de `AsignacionDeRecursos` no son evasiones: `esCompleta()` es un predicado positivo
que falla cerrado, y el constructor normaliza el identificador en blanco a nulo. Que se pueda representar
una asignación incompleta es deliberado: es lo que permite a `confirmarProgramacion` lanzar por **VIA-01**.

### Cierre de `S1b-consolidacion`

Implementado por Claude. `ConsolidacionTest` añade doce casos y **diez de ellos fallan contra el commit de
`S1a`**, que es la prueba de que las invariantes no estaban: con los huecos devolviendo `true`, consolidar
una carga que excede la capacidad, que va por otro corredor o que el contrato prohíbe no lanzaba nada.

Los otros dos casos —la matriz de compatibilidad en las nueve combinaciones y la secuencia de estiba— usan
API que `S1a` no tenía (`TipoDeCarga.esCompatibleCon`, `Parada.CARGA`/`DESCARGA`), así que contra ese commit
ni siquiera compilan. No cuentan como demostración.

Dos decisiones de diseño:

- **`secuenciaDeEstiba()` filtra las paradas de tipo `DESCARGA`.** Una parada de carga no tiene orden de
  descarga contra el que estibar. El vocabulario `CARGA`/`DESCARGA` lo fija el contrato 4, no esta clase:
  es la diferencia entre leer el contrato e inventarse un protocolo de texto, que es justo lo que hubo que
  corregir en `msvc-comercial`.
- **La compatibilidad vive en `TipoDeCarga`, no en `Carga`.** `Carga.esCompatibleCon` delega. La regla es
  del tipo de carga, no de una carga concreta, y así la matriz se prueba sin construir cargas.

`consolidarOrden` evalúa VIA-07 → VIA-04 → VIA-03 → VIA-05 → VIA-02 y **muta al final**. La prueba
`elRechazoNoMutaNada` comprueba que un rechazo por capacidad no deja la orden a medio registrar.

### Notas de `S2-persistencia`

Los cinco objetos de valor con colección —`CargaConsolidada`, `HojaDeRuta`, `ClausulaDeConsolidacion`,
`ElegibilidadDeRecurso` y `AsignacionDeRecursos`— pasan a clase inmutable. Aquí **sí** son
`@ElementCollection`, y funciona, porque cada uno se embebe **una sola vez** en `Viaje`. En
`msvc-comercial` no se pudo, porque allí la misma `Tarifa` se embebe tres veces.

**Un objeto de valor con colección nunca vuelve nulo de la base.** Hibernate instancia la colección
vacía, así que el embebido deja de parecer ausente: un viaje sin hoja de ruta volvía con una `HojaDeRuta`
de cero paradas en vez de `null`, y **VIA-01** se evalúa contra ese `null`. Se normaliza al cargar con
`@PostLoad`, **en un solo sitio**: la versión entregada duplicaba la misma condición en los accesores, y
dos copias de una condición derivan.

Las agendas llevan índice por recurso y ventana (`ix_reservas_unidades_solape`), que es lo que hará
eficiente la comprobación de **AGU-01** y **AGC-01** cuando `S3` la consulte.

## Slice `S3-api-publica` — decisiones de diseño

La receta común está en [§8 del método de trabajo](../README.md#8-receta-de-s3-api-publica) y el módulo
de referencia, ya terminado, es **`msvc-conductores`**. Se copia su forma exacta: `RelojConfig`,
`RecursoNoEncontradoException`, `ConflictoDeRecursoException`, `ManejadorDeErrores`, servicios de
aplicación concretos, DTO `record`, mapeadores estáticos de una sola dirección.

Aquí sólo va lo propio de este contexto.

### Mapa de excepciones a códigos HTTP

`DominioProgramacionException` queda de comodín en `422`. Las demás se listan una a una, y Spring elige siempre
la más específica.

| Excepción | Código | Por qué |
|---|---:|---|
| `TransicionDeViajeInvalidaException` | `409` | El viaje no está en el estado que la operación exige. |
| `ViajeDespachadoException` | `409` | VIA-07. Despachado no admite órdenes nuevas. |
| `AsignacionIncompletaException` | `409` | VIA-01. Faltan unidad o conductores; con los recursos asignados la misma petición vale. |
| `ReservaSolapadaException` | `409` | AGU-01 y AGC-01. La ventana está tomada *ahora*. |
| `RecursoNoElegibleException` | `409` | AGU-02 y AGC-02. El proveedor dice que no es elegible en esa fecha. |
| `CapacidadExcedidaException` | `422` | VIA-02. La carga no cabe: el cuerpo está mal. |
| `CorredorIncompatibleException` | `422` | VIA-03. |
| `ConsolidacionProhibidaException` | `422` | VIA-04. |
| `CargaIncompatibleException` | `422` | VIA-05. |

Además, en todos los módulos: `RecursoNoEncontradoException` → `404`, `ConflictoDeRecursoException` →
`409`, `IllegalArgumentException` → `400`, y la validación de forma → `400` con el detalle campo a campo
bajo la clave `errores`.

### Servicios de aplicación

Uno por raíz de agregado, con el nombre del agregado: ViajeService, AgendaService.
Ninguno decide reglas: cargan, llaman al método del agregado y guardan. Las dos únicas comprobaciones
admitidas son la existencia (`404`) y la unicidad contra el repositorio (`409`).

### El `404` que las tablas de arriba no escriben

Toda ruta con `{id}` puede devolver `404`, se diga o no en la columna de códigos: pedir un subrecurso
de un agregado que no existe no es un `400`. Las tablas de la API de este documento se escribieron en
`S1` y omiten ese caso; el código no lo omite.

## Slice `S5-clientes` — decisiones de diseño

Tres clientes, y el slice que más cambió la API pública del Core. No por los clientes en sí, sino por
lo que se vio al conectarlos.

### Dos invariantes se comprobaban contra datos del solicitante

| Operación | Qué traía la petición | Qué decide ese dato |
|---|---|---|
| `consolidarOrden` | la cláusula del contrato marco | **VIA-04**, si se puede consolidar |
| `asignarRecursos` | la elegibilidad de unidad y conductores | si los recursos se pueden reservar |

En los dos casos quien pedía la operación aportaba también el dato que la autoriza. Enviar una cláusula
permisiva bastaba para que VIA-04 no pudiera fallar nunca. Ahora la orden entera la trae el contrato 1
y las elegibilidades los contratos 2 y 3.

Lo que sí sigue viniendo de la petición, y es correcto:

- **`secuenciaDeDescarga`** — dónde va esta orden en la estiba. Comercial no sabe con qué otras órdenes
  va a compartir plataforma; `CargaConsolidada` ordena por este campo.
- **`capacidadDeLaUnidad`** — hace falta antes de que haya unidad asignada, porque consolidar precede a
  asignar en el ciclo de vida del viaje.

### Dos defectos del gateway que ninguna prueba veía

**El `TipoDeCarga` se deducía de prefijos de texto.** `"PALLETS"` en el embalaje daba `PALETIZADA`,
`"MAQUINARIA"` en la naturaleza daba `MAQUINARIA_PESADA`, y todo lo demás caía en `GENERAL`. VIA-05 se
decide con ese campo y la maquinaria pesada es la única que no comparte plataforma con nada: una
naturaleza escrita de otra forma la convertía en carga general consolidable con cualquier cosa. El
contrato 1 publica ahora el tipo. Es el mismo defecto que CTM-02 tuvo en `S1`.

**La `Carga` se montaba con `secuenciaDeDescarga = 1` fija**, así que todas las órdenes de un viaje
consolidado empataban en el primer puesto de la estiba.

### `CargaConsolidada.tipoDominante()`

El tipo que se le pregunta a Unidades por el contrato 2 es **el más restrictivo de los que van a
bordo**, no el primero ni el más frecuente. Si viaja una maquinaria pesada, la unidad tiene que poder
con maquinaria pesada aunque el resto sea carga general. Es VIA-05 vista desde el otro lado.

### Pruebas exigidas por este slice

Además de los tres `GatewayTest` y los tres `ClientStubTest`, tres en `ViajeServiceTest`: que la
cláusula consultada manda, que los dos proveedores se consultan de verdad, y que con Unidades caída no
se reserva nada. Sin la última, «no se supone elegible lo que no se pudo comprobar» es una frase.
