# Método de trabajo

Este documento define cómo se construye el sistema: quién decide, quién programa, y cuándo algo está terminado.
El reparto de roles resumido vive en [`CLAUDE.md`](../../CLAUDE.md); aquí está el detalle operativo.

## 1. Por qué hay un orquestador

Los siete microservicios comparten un diseño táctico con 48 invariantes de negocio. La mayor parte del trabajo
—entidades, objetos de valor, repositorios, DTO, mappers, controladores, clientes Feign y sus pruebas— es
mecánica una vez que el contrato está escrito. Lo que no es mecánico es decidir cuál es el contrato, qué
invariante protege qué agregado, y si el resultado sirve.

Por eso el trabajo se divide en dos capas:

- **Decisión** (Claude): dominio, contratos, criterios de éxito, revisión, integración.
- **Ejecución** (agy / Gemini vía el subagente `antigravity-delegate`): escritura de código y pruebas contra una
  spec cerrada.

Un agente ejecutor nunca recibe un objetivo de negocio. Recibe una spec y una lista de criterios verificables.

### Qué pasó con el reparto por persona

El plan inicial asignaba un microservicio a cada integrante como **quien lo programa**. Ahora lo programan los
agentes. El reparto no desaparece: cambia de significado.

Cada integrante es el **responsable de revisión** de su contexto. Es quien conoce sus invariantes, quien
aprueba el pull request y quien responde por él en la sustentación. Que el código lo haya escrito un agente no
traslada esa responsabilidad.

Las ramas se conservan tal como las define [`CONTRIBUTING.md`](../../CONTRIBUTING.md), y ahora sirven para
más que antes:

- Aíslan lo que escribe un agente hasta que alguien lo verificó. **Nunca se delega sobre `main`.**
- Hacen que el diff sea la unidad de revisión, que es justo lo que un ejecutor no humano vuelve
  indispensable: la revisión deja de ser un trámite y pasa a ser el control de calidad principal.
- Dejan trazabilidad de quién aprobó qué, que es lo que el curso evalúa.

Un slice, una rama, un pull request, un revisor: el responsable del contexto.

## 2. La unidad de trabajo es el *slice*

Un slice es un corte vertical delgado sobre un solo microservicio: un agregado con sus invariantes, su
persistencia, su endpoint y sus pruebas. Nunca es «implementa msvc-comercial».

Un slice bien formado cumple:

- Toca **un solo módulo Maven**.
- Cierra **al menos una invariante** con su prueba.
- Deja el reactor **verde** (`./mvnw -pl <msvc> verify`).
- Es revisable en un diff que cabe en una sesión.

El orden de los slices está en [`backlog.md`](backlog.md).

## 3. Ciclo por slice

### 3.1 Claude prepara

1. Confirma que la spec del servicio (`servicios/msvc-<contexto>.md`) cubre el slice. Si no, la escribe primero.
2. Fija los criterios de aceptación del slice: invariantes por código, endpoints, códigos HTTP.
3. Crea la rama `feat/<msvc>/<slice>`. **Nunca se delega sobre `main`.**

### 3.2 Claude delega

Se usa el subagente `antigravity-delegate`. La plantilla de prompt está en §5.

Elección de tier:

| Tier | Cuándo |
|---|---|
| `flash` | Scaffolding, DTO, mappers, repositorios, CRUD, pruebas repetitivas. Es el caso normal. |
| `pro` | Lógica con varias invariantes acopladas: consolidación de cargas, secuencia de estiba, aplicación de pagos. |
| *no delegar* | Menos de ~30 líneas en 1–2 archivos: sale más caro el viaje de ida y vuelta que hacerlo. |

### 3.3 Claude verifica

**Nunca se acepta el auto-reporte de agy.** El orden de verificación es:

```bash
git status --short
```

```bash
./mvnw -pl <msvc> verify
```

`verify` exige Docker: Failsafe levanta un MySQL real con Testcontainers para las pruebas `*IT`. Sin Docker,
`./mvnw -pl <msvc> test` cubre sólo el arranque de contexto y **no vale como gate**.

Después, revisión manual del diff contra:

- las invariantes declaradas en el slice,
- la frontera de contexto (sin imports cruzados, sin entidades JPA en la firma de un controlador),
- los códigos HTTP y el `application/problem+json`.

**Antes de tocar nada, se commitea la salida cruda de agy.** Un commit `feat(agy): ...` con lo que
el agente escribio, sin editar, y despues un commit `fix: ...` con las correcciones de Claude. Si se
corrige encima sin commitear, el diff de revision mezcla las dos manos y ya no se puede ver que hizo
el agente ni medir si delegar compenso. Es la unica forma de que el pull request sea auditable.

### 3.4 Claude cierra o corrige

- **Pasa**: se marca el slice como Done en `backlog.md` y se integra a `main`.
- **Falla parcial**: Claude corrige puntualmente. No se re-delega una spec entera por un fallo local.
- **Falla estructural** (agy entendió otra cosa): la spec estaba ambigua. Claude la corrige y re-delega.
  El defecto es de la spec, no del ejecutor.

## 4. Definition of Done

### 4.1 De un slice

- [ ] `./mvnw -pl <msvc> verify` en verde.
- [ ] Cada invariante del slice tiene una prueba que la **viola** y espera el fallo.
- [ ] Los endpoints del slice responden los códigos documentados en la spec.
- [ ] Los errores salen como `application/problem+json`.
- [ ] Ningún import de otro contexto.
- [ ] Ninguna entidad JPA en la firma de un controlador ni en un DTO.
- [ ] `backlog.md` actualizado.

### 4.2 De un microservicio

Un microservicio está terminado cuando cumple los siete criterios siguientes. Son objetivos y comprobables;
no hay un criterio de «calidad» subjetivo.

| # | Criterio | Cómo se comprueba |
|---|---|---|
| 1 | Compila y pasa | `./mvnw -pl <msvc> verify` |
| 2 | Invariantes cubiertas | Todas las de su tabla en `invariantes.md` tienen prueba en verde |
| 3 | Agregados completos | Cada agregado del diseño táctico existe con su raíz, entidades hijas y VO |
| 4 | Contrato publicado | Sus endpoints `/internal/v1` existen y están en `docs/api/contracts.md` |
| 5 | Contrato consumido | Cada cliente Feign que necesita tiene timeout, traducción de error y prueba con stub |
| 6 | Aislado | 0 imports de otro contexto; 0 FK a otro esquema |
| 7 | Arranca | Aparece sano en `./scripts/smoke-test.sh` |

### 4.3 Del sistema

El flujo vertical completo del negocio corre de extremo a extremo:

orden confirmada en Comercial → viaje programado con unidad y conductor elegibles → hoja de ruta ejecutada
con check-list y conformidad → factura emitida → cuenta por cobrar creada y pago aplicado.

## 5. Plantilla de delegación

Todo prompt a `antigravity-delegate` tiene esta forma. Lo que no está en la plantilla, no se delega.

```
CONTEXTO
Repo: monorepo Maven, Java 26, Spring Boot 4.1.0. Trabajas SOLO en msvc-<contexto>.
Lee primero: docs/delivery/servicios/msvc-<contexto>.md
             docs/api/contracts.md (sección <contexto>)

TAREA
<un slice, descrito en una frase>

ARCHIVOS A CREAR
<lista explícita de rutas>

REGLAS DURAS
- No toques ningún otro módulo msvc-*.
- No añadas dependencias al pom.xml.
- Ninguna entidad JPA en un DTO ni en la firma de un controlador.
- Referencias a otros contextos: identificador escalar, nunca relación JPA.
- Errores con application/problem+json.

INVARIANTES A CUBRIR
<códigos, p. ej. VIA-01, VIA-02> — por cada una, una prueba que la viole y espere el fallo.

CRITERIO DE ACEPTACIÓN
./mvnw -pl msvc-<contexto> verify en verde.

ENTREGABLE
Digest: archivos creados, invariantes cubiertas, pruebas añadidas. Sin volcados de código.
```

## 6. Reglas de dominio

Salieron de revisar los slices de la Ola 1. **Cada una corresponde a un defecto real que llegó con todas
las pruebas en verde.** Son normativas para los siete contextos y toda spec de slice las da por incluidas.

### D1 · El dominio no lee el reloj

Ni un `LocalDate.now()` ni un `Instant.now()` en `src/main`. Toda operación que dependa de «hoy» recibe la
fecha y la exige no nula. Prohibida la sobrecarga sin fecha que la deduzca por dentro.

*Origen:* `Unidad.estaHabilitada()` tenía una sobrecarga con reloj implícito — una prueba no determinista
esperando a romper el CI.

### D2 · Ninguna invariante se evade pasando `null`

Prohibido `if (x != null && !cumple) { fallar; }`: deja pasar el caso nulo. Si un dato hace falta para
evaluar una invariante, es obligatorio y su ausencia lanza.

*Origen:* `OrdenDeMantenimiento.abrir()` aceptaba `kmUltimoMantenimiento` nulo y se saltaba OMT-02 entera.

### D3 · Ningún constructor de escape

Si una invariante compara importes o cantidades que llegan de fuera, **se reciben todos**. Deducir uno de
los otros hace que la igualdad se cumpla por construcción y la comprobación no pueda fallar jamás.

*Origen:* `CuentaPorCobrar` deducía `montoNeto = total − detraccion`, así que FAC-04 era indemostrable por
esa vía. La usaban 34 de 35 llamadas.

### D4 · Nada de valores por defecto silenciosos

La moneda de un importe es obligatoria. Ni `"PEN"` por defecto, ni deducirla del primer elemento de una
lista, ni reventar cuando la lista está vacía.

*Origen:* `deudaTotal()` adivinaba la moneda y lanzaba `IllegalStateException` con la cartera vacía — que
es el primer caso que responde el contrato 11.

### D5 · Cuidado con los rangos inclusivos

Un periodo `[desde, hasta]` inclusivo en ambos extremos convierte una ventana de un día en una de dos.
Los bordes se prueban uno a uno contra la tabla, nunca se dan por hechos.

*Origen:* la ventana de conducción duraba cuarenta y ocho horas en vez de veinticuatro, y la prueba estaba
escrita contra el defecto: usaba `plusDays(2)` para «salir de la ventana».

### D6 · Se valida todo antes de mutar nada

Una operación que toca dos agregados comprueba todas sus condiciones primero y sólo entonces muta. Ninguna
de las dos partes puede quedar a medias.

### D7 · Ningún parámetro muerto

Un argumento que se valida y no se usa miente sobre lo que hace el método, y el slice siguiente lo copia
al controlador.

*Origen:* `Conductor.registrarInduccion(induccion, fecha)`.

### D8 · Lo derivado se calcula

`saldo()`, `total()`, `montoNeto()`, `deudaTotal()` y compañía se calculan en el momento. Un campo
persistido que duplica un cálculo es un defecto, y en `LiquidacionDeViaje` lo dice la propia LIQ-02.

## 7. Receta de `S2-persistencia`

Probada de extremo a extremo en `msvc-conductores` contra MySQL 8.4 real antes de delegarse. Con
`ddl-auto=validate` no hay margen: una entidad sin su migración, o con una columna que no cuadra, rompe
el build. Es lo buscado.

### El agregado sigue siendo rico

Se conserva el constructor que valida las invariantes y se **añade** uno `protected` sin argumentos para
JPA, documentado como no utilizable. Los campos persistidos **dejan de ser `final`**: Hibernate sustituye
la colección por su propia implementación y en Java 26 escribir un campo final por reflexión es frágil.

```java
/** Exigido por JPA. No usar: no valida ninguna invariante. */
protected Conductor() {
}
```

### Los objetos de valor son `record` y se mapean tal cual

Hibernate 7 admite `record` como `@Embeddable` sin envoltorio. No se convierten a clase.

### Un mismo tipo de VO dos veces en la misma entidad exige renombrar columnas

Tres `PeriodoDeVigencia` en un agregado piden los tres las columnas `desde` y `hasta`, y el mapeo choca:

```java
@Embedded
@AttributeOverrides({
    @AttributeOverride(name = "desde", column = @Column(name = "licencia_desde", nullable = false)),
    @AttributeOverride(name = "hasta", column = @Column(name = "licencia_hasta", nullable = false))
})
private PeriodoDeVigencia vigenciaLicencia;
```

### Un embebido anidado se direcciona con la ruta con punto

`HorasDeConduccion` contiene a su vez un `PeriodoDeVigencia`:

```java
@AttributeOverride(name = "ventanaDeComputo.desde", column = @Column(name = "ventana_desde"))
```

### La entidad hija pertenece al agregado

`cascade = ALL` y `orphanRemoval = true` traducen esa pertenencia. **No lleva repositorio propio**: se
alcanza por la raíz, que es lo que significa ser entidad hija.

```java
@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
@JoinColumn(name = "conductor_id", nullable = false)
private List<Induccion> inducciones = new ArrayList<>();
```

### Claves foráneas sólo dentro del contexto

Una FK entre `inducciones` y `conductores` es correcta: mismo esquema, mismo agregado. Un `clienteId`
es un identificador escalar **sin FK**: el cliente vive en otro contexto y otro esquema (regla 3).

### La migración

`src/main/resources/db/migration/V1__crear_esquema_<contexto>.sql`, en el módulo dueño. `ENGINE = InnoDB`,
`CHARSET = utf8mb4`, restricciones nombradas (`pk_`, `fk_`, `uq_`, `ix_`). Los tipos tienen que casar con
lo que Hibernate espera: `VARCHAR` para `String` y enums `STRING`, `DECIMAL(p,s)` para `BigDecimal`,
`DATE` para `LocalDate`, `DATETIME(6)` para `OffsetDateTime`.

### La prueba de integración deja de ser un trámite

`Persistencia<Ctx>IT` ya no comprueba sólo que exista `flyway_schema_history`. **Guarda el agregado
completo, limpia el contexto de persistencia y lo relee**, y verifica que los objetos de valor, el
embebido anidado y las entidades hijas sobreviven al viaje de ida y vuelta. Sin el `entityManager.clear()`
la prueba lee de la caché de primer nivel y no demuestra nada.

## 8. Medición

Cada delegación deja su consumo en `~/.claude/agy-usage.log` (`AGY_USAGE`). Sirve para saber si delegar
un tipo de slice compensa. Si un slice necesita más de dos rondas de corrección, deja de compensar: la
spec era mala o el trabajo no era mecánico.
