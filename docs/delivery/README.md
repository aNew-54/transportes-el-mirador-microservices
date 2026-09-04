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

## 6. Medición

Cada delegación deja su consumo en `~/.claude/agy-usage.log` (`AGY_USAGE`). Sirve para saber si delegar
un tipo de slice compensa. Si un slice necesita más de dos rondas de corrección, deja de compensar: la
spec era mala o el trabajo no era mecánico.
