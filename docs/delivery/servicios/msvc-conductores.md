# msvc-conductores — Gestión de Conductores

| | |
|---|---|
| Bounded context | Gestión de Conductores |
| Subdominio | Support |
| Puerto | `8050` |
| Esquema | `mirador_conductores` |
| Paquete raíz | `pe.edu.unc.elmirador.conductores` |
| Responsable de revisión | Brayam Alfaro |
| Agregados | 1 |
| Invariantes | 3 (CON-01, CON-02, CON-03) |

## Responsabilidad

Controlar la vigencia y categoría de la licencia de cada uno de los dieciocho conductores, la vigencia de las
inducciones de seguridad que exigen los clientes mineros, el acumulado de horas de conducción y descansos, y
el historial de viajes e incidencias atribuibles.

Es el servicio más pequeño en superficie y uno de los más críticos en efecto: su única salida hacia el Core
decide si un conductor puede ser asignado.

Está en relación `Partnership` con `msvc-unidades`.

## Agregados

### `Conductor` — raíz `Conductor`, entidad hija `Inducción`

- **Objetos de valor**: `NúmeroDeLicencia`, `CategoríaDeLicencia`, `PeriodoDeVigencia`,
  `HorasDeConducción`, `EstadoDeHabilitación`
- **Referencias**: `ClienteId`, en la inducción exigida
- **Métodos**: `estáHabilitadoPara(tipoUnidad)`, `acumularHoras(horas)`
- **Invariantes**: CON-01, CON-02, CON-03

`CategoríaDeLicencia.habilitaPara(tipoDeUnidad)` decide qué camión puede conducir un chofer con esa
categoría. Es la contraparte de `TipoDeUnidad.licenciaRequerida()` en `msvc-unidades`; ambas deben
coincidir, y esa coincidencia se verifica en el contrato 3, no por acoplamiento entre módulos.

`HorasDeConducción` guarda `horas` y `ventanaDeCómputo`, con `tieneDisponibles(requeridas)`. El acumulado se
mide dentro del periodo normado por la regulación de descansos, no de forma indefinida.

`EstadoDeHabilitación` lleva `situacion` y `motivo`. Los motivos son los del contrato 3:
`LICENCIA_VENCIDA`, `CATEGORIA_INSUFICIENTE`, `HORAS_INSUFICIENTES`, `INDUCCION_VENCIDA:<clienteId>`.

`Inducción` referencia el `ClienteId` que la exige y tiene su propio `PeriodoDeVigencia`; se renueva
anualmente. Un conductor puede estar habilitado en general y no habilitado para un cliente concreto (CON-03):
la habilitación **no es un booleano global**.

## API pública `/api/v1`

| Método | Ruta | Qué hace | Códigos |
|---|---|---|---|
| `POST` | `/conductores` | Registra un conductor con su licencia | `201` `400` `409` |
| `GET` | `/conductores/{id}` | Consulta el legajo | `200` `404` |
| `GET` | `/conductores` | Lista con filtro por situación de habilitación | `200` |
| `POST` | `/conductores/{id}/licencia` | Renueva la licencia o cambia la categoría | `200` `400` `404` |
| `POST` | `/conductores/{id}/inducciones` | Registra una inducción para un cliente | `201` `400` `404` |
| `GET` | `/conductores/{id}/horas` | Consulta el acumulado en la ventana vigente | `200` `404` |
| `POST` | `/conductores/{id}/descanso` | Registra un descanso y libera horas | `200` `404` |
| `POST` | `/conductores/{id}/suspender` | Suspende con motivo obligatorio | `200` `400` `404` |
| `POST` | `/conductores/{id}/rehabilitar` | Rehabilita tras renovar la licencia | `200` `404` `409` (CON-01) |
| `GET` | `/alertas` | Licencias e inducciones por vencer | `200` |

Dos correcciones sobre la tabla que cerró `S1`, las dos detectadas al escribir `S3`:

- **Faltaba el `404` en las rutas de subrecurso.** Renovar la licencia de un conductor inexistente no es
  un `400`. El error estaba en el documento, no en el código.
- **Faltaban `suspender` y `rehabilitar`.** `Conductor.suspender(motivo)` y `Conductor.rehabilitar(fecha)`
  existen desde `S1` y ningún endpoint los alcanzaba: métodos de dominio muertos, que es la versión de
  agregado del defecto D7. `rehabilitar` es además el único camino de `S3` hasta
  `RehabilitacionInvalidaException`, y por tanto el único que ejercita el `409` del contexto.

Son dos endpoints y no un `POST /estado` con un campo `situacion` porque despachar sobre ese campo
metería un `if` en el servicio de aplicación. Dos rutas, cero ramas.

El `422` de este contexto no se alcanza desde `S3`: la única invariante que lo produce es CON-02, y las
horas entran por el contrato 6, que es de `S4`. El comodín del manejador se prueba contra el manejador
directamente, y se dice en el propio test por qué.

## API interna `/internal/v1`

Publica los contratos **3** y **6**.

| Método | Ruta | Consumidor | Contrato |
|---|---|---|---|
| `GET` | `/conductores/{conductorId}/elegibilidad` | Programación | 3 |
| `POST` | `/conductores/{conductorId}/horas-conduccion` | Ejecución | 6 |
| `POST` | `/conductores/{conductorId}/incidencias` | Ejecución | 6 |

El endpoint de elegibilidad concentra las tres invariantes. `clienteId` es opcional: sólo llega cuando el
destino exige inducción. Sin ese parámetro, CON-03 no se evalúa.

## Clientes Feign que consume

Ninguno. Conductores es un proveedor puro.

## Criterios de éxito

- [ ] `./mvnw -pl msvc-conductores verify` en verde (exige Docker: levanta MySQL con Testcontainers)
- [ ] Cada tabla del contexto creada por una migración Flyway; `ddl-auto=validate` en verde
- [ ] `PersistenciaConductoresIT` en verde contra MySQL real
- [ ] Las 3 invariantes con prueba que las viola
- [ ] `CategoríaDeLicencia.habilitaPara()` probado para las tres categorías contra los tres tipos de unidad
- [ ] Prueba de CON-03: mismo conductor elegible sin `clienteId` y no elegible con un `clienteId` cuya
      inducción está vencida
- [ ] `HorasDeConducción.tieneDisponibles()` probado en el borde exacto del máximo normado
- [ ] `POST .../horas-conduccion` devuelve `409` cuando el acumulado superaría el máximo (CON-02)
- [ ] Idempotencia probada en los dos `POST` del contrato 6
- [ ] 0 imports de otro contexto
- [ ] Sano en `./scripts/smoke-test.sh`

---

## Slice `S1-dominio` — decisiones de diseño

Sólo modelo de dominio y pruebas. Sin `@Entity`, sin repositorios, sin controladores, sin migraciones.
Los objetos de valor sí llevan `@Embeddable`: la anotación es inerte mientras ninguna entidad los referencie.

### Reglas heredadas de la revisión de `msvc-unidades`

Son normativas para este slice y para los cinco contextos que faltan:

1. **El dominio no lee el reloj.** Ningún `LocalDate.now()`. Toda operación que dependa de «hoy» recibe la
   fecha y la exige no nula.
2. **Ninguna invariante se evade pasando `null`.** Si un dato es necesario para evaluar una invariante, es
   obligatorio en la firma. Un parámetro nulo que desactiva la comprobación no es tolerancia: es un agujero.
3. **Nada de valores por defecto silenciosos** en datos del negocio.
4. Identificadores ASCII (regla 13): `estaHabilitadoPara`, `estaVigente`, `Induccion`, `NumeroDeLicencia`.

### Correspondencia con el diseño táctico

| Diseño táctico | Código |
|---|---|
| `estáHabilitadoPara(tipoUnidad)` | `estaHabilitadoPara(...)` |
| `Inducción` | `Induccion` |
| `HorasDeConducción` | `HorasDeConduccion` |
| `CategoríaDeLicencia` | `CategoriaDeLicencia` |
| `PeriodoDeVigencia.estáVigente()` | `estaVigenteEn(LocalDate)` |

### Objetos de valor — `models/vo`

| Tipo | Forma | Comportamiento |
|---|---|---|
| `NumeroDeLicencia` | `record NumeroDeLicencia(String valor)` | Normaliza a mayúsculas. Formato peruano: una letra y ocho dígitos (`Q12345678`); otro valor lanza `NumeroDeLicenciaInvalidoException` |
| `PeriodoDeVigencia` | `record PeriodoDeVigencia(LocalDate desde, LocalDate hasta)` | `hasta` posterior a `desde`. `estaVigenteEn(LocalDate)`, `venceDentroDe(int dias, LocalDate ref)`. Ambos exigen fecha no nula |
| `HorasDeConduccion` | `record HorasDeConduccion(BigDecimal horas, PeriodoDeVigencia ventanaDeComputo)` | `horas` no negativa, escala 2. `tieneDisponibles(BigDecimal requeridas)`, `disponibles()`, `acumular(BigDecimal)` que lanza `HorasExcedidasException` si supera el máximo |
| `EstadoDeHabilitacion` | `record EstadoDeHabilitacion(SituacionDeHabilitacion situacion, String motivo)` | `estaHabilitado()` ⇔ `HABILITADO`. Toda situación distinta exige motivo no vacío. Fábricas `habilitado()`, `suspendido(motivo)` |

Enumeraciones:

| Enum | Valores | Comportamiento |
|---|---|---|
| `CategoriaDeLicencia` | `A_IIIA` · `A_IIIB` · `A_IIIC` | `habilitaPara(TipoDeUnidad)` |
| `TipoDeUnidad` | `FURGON` · `PLATAFORMA` · `CAMA_BAJA` | — |
| `SituacionDeHabilitacion` | `HABILITADO` · `SUSPENDIDO` | — |
| `MotivoDeNoElegibilidad` | `LICENCIA_VENCIDA` · `CATEGORIA_INSUFICIENTE` · `HORAS_INSUFICIENTES` · `INDUCCION_VENCIDA` · `NO_HABILITADO` | `codigo()` y `codigo(String detalle)` → `"INDUCCION_VENCIDA:CLI-0019"` |

**`TipoDeUnidad` y `CategoriaDeLicencia` se duplican respecto de `msvc-unidades` a propósito.** Son contextos
distintos y no comparten código; no existe módulo común y no se va a crear. La contraparte de
`TipoDeUnidad.licenciaRequerida()` es `CategoriaDeLicencia.habilitaPara()`, y su coincidencia se verifica en
el contrato 3, nunca por un import.

**Tabla de decisión.** La categoría superior habilita todo lo que habilita la inferior:

| `habilitaPara` | `FURGON` | `PLATAFORMA` | `CAMA_BAJA` |
|---|:---:|:---:|:---:|
| `A_IIIA` | sí | no | no |
| `A_IIIB` | sí | sí | no |
| `A_IIIC` | sí | sí | sí |

Es exactamente la inversa de la tabla `licenciaRequerida()` de `msvc-unidades` (`FURGON` → `A_IIIA`,
`PLATAFORMA` → `A_IIIB`, `CAMA_BAJA` → `A_IIIC`). Si una de las dos cambia, la otra queda inconsistente y
sólo lo detecta la prueba del contrato 3: por eso las nueve celdas van probadas una a una en ambos lados.

**Máximo normado de horas.** `HorasDeConduccion.MAXIMO_HORAS` = `10.00` en una ventana de 24 horas, según
el reglamento nacional de administración de transporte (DS 017-2009-MTC), que limita a cinco horas de
conducción continua y diez en un periodo de veinticuatro. Es una constante del dominio, no un parámetro de
configuración: si el negocio la cambia, se cambia aquí y las pruebas de borde caen solas.

### Agregado `Conductor` — `models/entity`

Raíz `Conductor`, entidad hija `Induccion`. Identidad y referencias como `String`, nunca objetos.

Campos: `id`, `nombreCompleto`, `NumeroDeLicencia`, `CategoriaDeLicencia`, `PeriodoDeVigencia vigenciaLicencia`,
`HorasDeConduccion horasAcumuladas`, `EstadoDeHabilitacion estado`, `List<Induccion> inducciones`.

| Método | Contrato |
|---|---|
| `estaHabilitadoPara(LocalDate fecha, TipoDeUnidad tipo, BigDecimal horasRequeridas, String clienteId)` | `false` si el estado no está habilitado, si la licencia no está vigente (**CON-01**), si la categoría no habilita ese tipo (**CON-01**), si no tiene horas disponibles (**CON-02**) o si `clienteId` no es nulo y su inducción falta o venció (**CON-03**) |
| `motivosDeNoElegibilidad(LocalDate, TipoDeUnidad, BigDecimal horasRequeridas, String clienteId)` | Los motivos del contrato 3 en orden estable; lista vacía ⇔ elegible. `clienteId` nulo significa que el destino no exige inducción: **CON-03 no se evalúa**, y ésa es la única forma legítima de omitirla |
| `acumularHoras(BigDecimal horas, LocalDate fecha)` | **CON-02**: si el acumulado superara `MAXIMO_HORAS`, lanza `HorasExcedidasException`. Si la fecha cae fuera de la ventana de cómputo, la ventana se renueva y el acumulado vuelve a cero |
| `registrarDescanso(LocalDate fecha)` | Reinicia el acumulado abriendo una ventana nueva desde esa fecha |
| `renovarLicencia(NumeroDeLicencia, CategoriaDeLicencia, PeriodoDeVigencia)` | Reemplaza licencia y categoría |
| `registrarInduccion(Induccion, LocalDate fecha)` | Reemplaza la inducción del mismo `clienteId`. Registrar una ya vencida no habilita |
| `suspender(String motivo)` / `rehabilitar(LocalDate fecha)` | `rehabilitar` falla con `RehabilitacionInvalidaException` si la licencia sigue vencida |

`Induccion` — entidad hija: `id`, `clienteId`, `PeriodoDeVigencia`, `estaVigenteEn(LocalDate)`.

`clienteId` es un `String` escalar. **No** hay entidad Cliente en este contexto.

### Excepciones — `exceptions`

Raíz `DominioConductoresException`; herederas `NumeroDeLicenciaInvalidoException`, `HorasExcedidasException`,
`RehabilitacionInvalidaException`. Traducirlas a `problem+json` es trabajo de `S3`.

### Pruebas exigidas por este slice

JUnit 5 puro, sin `@SpringBootTest`, en `src/test/java/pe/edu/unc/elmirador/conductores/models/`.

| Invariante | Prueba mínima |
|---|---|
| **CON-01** | Licencia vencida ⇒ no elegible, motivo `LICENCIA_VENCIDA`. Y categoría insuficiente con licencia vigente ⇒ motivo `CATEGORIA_INSUFICIENTE`. Son dos casos distintos, no uno |
| **CON-02** | `acumularHoras` que cruzaría el máximo lanza y **no** altera el acumulado. En el borde exacto del máximo no lanza |
| **CON-03** | El **mismo** conductor: elegible con `clienteId` nulo, y no elegible con un `clienteId` cuya inducción está vencida, motivo `INDUCCION_VENCIDA:<clienteId>`. Más el caso de inducción ausente |

Bordes obligatorios:

- `habilitaPara` en las 9 combinaciones de la tabla.
- `tieneDisponibles` en el borde exacto de `MAXIMO_HORAS`, y un caso por encima y otro por debajo.
- `registrarDescanso` libera horas y vuelve a hacer elegible a un conductor que no lo era.
- `acumularHoras` con fecha fuera de la ventana renueva la ventana en vez de lanzar.
- `motivosDeNoElegibilidad` acumulando dos motivos, y lista vacía en el caso elegible.
- Toda operación con fecha nula lanza `IllegalArgumentException` (el dominio no lee el reloj).
- `NumeroDeLicencia` rechaza formato inválido y normaliza minúsculas.

### Correcciones tras la revisión de `S1-dominio`

**La ventana de cómputo dura veinticuatro horas, no cuarenta y ocho.** `PeriodoDeVigencia.estaVigenteEn`
es inclusivo en los dos extremos, así que aplicarlo sobre `[fecha, fecha+1]` hacía que el día siguiente
siguiera contando como el mismo periodo. Un conductor que agotaba sus diez horas el lunes seguía sin horas
el martes, y en el contrato 3 aparecía como no elegible con motivo `HORAS_INSUFICIENTES` sin que nadie
pudiera explicar por qué.

La ventana la decide ahora el objeto de valor: `HorasDeConduccion.cubre(LocalDate)` compara contra
`desde`, y `HorasDeConduccion.ventanaDe(LocalDate)` abre la del día. El agregado ya no construye periodos
a mano.

La prueba que lo detecta —`ConductorHorasTest`, caso de fecha fuera de la ventana— falla contra la versión
anterior con `expected: 5.00 but was: 9.00`. `VentanaDeConduccionTest` añade tres casos más.

Otras dos correcciones menores:

| Antes | Ahora | Por qué |
|---|---|---|
| `registrarInduccion(induccion, fecha)` | `registrarInduccion(induccion)` | La fecha se validaba y no se usaba. Un parámetro muerto miente sobre lo que hace el método, y S3 lo habría copiado al controlador. CON-03 no es estado almacenado: se evalúa por cliente al consultar la elegibilidad |
| `buscarInduccion` público | privado | El agregado decide; no expone su búsqueda interna. La prueba pasa a comprobar el efecto observable |

**Decisión de agy que se acepta:** inducción ausente e inducción vencida emiten el mismo motivo
`INDUCCION_VENCIDA:<clienteId>`. El contrato 3 no define `INDUCCION_AUSENTE` y el enum de motivos es
normativo; inventar un código habría roto al consumidor.
