# Backlog de slices

Orden de construcción y estado. Es el único lugar donde se marca algo como terminado, y sólo Claude lo marca,
después de correr el gate.

Estados: `pendiente` · `en curso` · `en revisión` · **`done`**

## Por qué este orden

Se construye **de proveedor a consumidor**. Un servicio no se empieza hasta que existen los contratos que
necesita consumir, de modo que sus clientes Feign se prueben contra un endpoint real y no sólo contra un stub.

```
Ola 1   Unidades · Conductores · Cobranza      proveedores puros, sin clientes Feign
Ola 2   Comercial                              consume Cobranza
Ola 3   Facturación                            consume Comercial y Cobranza
Ola 4   Programación (Core)                    consume Comercial, Unidades y Conductores
Ola 5   Ejecución                              consume los cinco anteriores
```

Ejecución va al final porque es el único servicio que consume a todos los demás y no provee a ninguno por
consulta. Programación va antes que Ejecución aunque sea el Core: sin hoja de ruta no hay nada que ejecutar.

## Forma de un slice

Cada servicio se descompone en la misma secuencia. La numeración es estable.

| Slice | Qué entrega | Tier |
|---|---|---|
| `S1-dominio` | Objetos de valor, agregados, invariantes y sus pruebas. Sin JPA, sin HTTP. | `flash`, salvo el Core |
| `S2-persistencia` | Mapeo JPA, repositorios y la migración Flyway `V1` del esquema | `flash` |
| `S3-api-publica` | Controladores `/api/v1`, DTO, validación, Problem Details | `flash` |
| `S4-api-interna` | Endpoints `/internal/v1` de los contratos que publica | `flash` |
| `S5-clientes` | Clientes Feign, timeouts, traducción de error, pruebas con stub | `flash` |

`S1` es el slice que más importa: es donde viven las invariantes. Se hace primero y no se avanza a `S2` hasta
que sus pruebas están en verde.

Cada slice vive en su rama `feat/<msvc>/<slice>` y se integra por pull request. Lo aprueba el responsable de
revisión del contexto, no quien lanzó la delegación. Con un ejecutor no humano, la revisión del diff deja de
ser un trámite y pasa a ser el control de calidad principal.

---

## Ola 1 — proveedores puros

### msvc-unidades · revisa Arnold Ocas

| Slice | Invariantes | Estado |
|---|---|---|
| `S1-dominio` | UNI-01, UNI-02, UNI-03, OMT-01, OMT-02, REP-01 | **done** |
| `S2-persistencia` | — | **done** |
| `S3-api-publica` | — | pendiente |
| `S4-api-interna` | contratos 2 y 5 | pendiente |

### msvc-conductores · revisa Brayam Alfaro

| Slice | Invariantes | Estado |
|---|---|---|
| `S1-dominio` | CON-01, CON-02, CON-03 | **done** |
| `S2-persistencia` | — | **done** |
| `S3-api-publica` | — | pendiente |
| `S4-api-interna` | contratos 3 y 6 | pendiente |

### msvc-cobranza · revisa María Belén Vilca

| Slice | Invariantes | Estado |
|---|---|---|
| `S1-dominio` | CCC-01, CCC-02, CCC-03, PAG-01, PAG-02 | **done** |
| `S2-persistencia` | — | **done** |
| `S3-api-publica` | — | pendiente |
| `S4-api-interna` | contratos 10 y 11 | pendiente |

---

## Ola 2 — Comercial

### msvc-comercial · revisa Sarah Herrera

| Slice | Invariantes | Estado |
|---|---|---|
| `S1-dominio` | CLI-01, COT-01, COT-02, ORD-01, ORD-02, CTM-01, CTM-02, TAR-01 | **done** |
| `S2-persistencia` | — | pendiente |
| `S3-api-publica` | — | pendiente |
| `S4-api-interna` | contratos 1, 7 y 9 | pendiente |
| `S5-clientes` | contrato 11 → Cobranza | pendiente |

---

## Ola 3 — Facturación

### msvc-facturacion · revisa María Belén Vilca

| Slice | Invariantes | Estado |
|---|---|---|
| `S1-dominio` | FAC-01, FAC-02, FAC-03, FAC-04, FAC-05, NCR-01 | **done** |
| `S2-persistencia` | — | **done** |
| `S3-api-publica` | — | pendiente |
| `S4-api-interna` | contrato 8 | pendiente |
| `S5-clientes` | contratos 9 → Comercial, 10 → Cobranza | pendiente |

---

## Ola 4 — Programación (Core Domain)

### msvc-programacion · revisa Brayam Alfaro

Único servicio con un slice partido en dos: la consolidación no se delega.

| Slice | Invariantes | Quién | Estado |
|---|---|---|---|
| `S1a-dominio-base` | AGU-01, AGU-02, AGC-01, AGC-02, VIA-01, VIA-07 | agy `flash` | **done** |
| `S1b-consolidacion` | VIA-02, VIA-03, VIA-04, VIA-05, VIA-06 | **Claude** | **done** |
| `S2-persistencia` | — | agy `flash` | pendiente |
| `S3-api-publica` | — | agy `flash` | pendiente |
| `S4-api-interna` | contrato 4 | agy `flash` | pendiente |
| `S5-clientes` | contratos 1, 2, 3 | agy `flash` | pendiente |

---

## Ola 5 — Ejecución

### msvc-ejecucion · revisa Alexander Infante

| Slice | Invariantes | Estado |
|---|---|---|
| `S1-dominio` | EJV-01…05, LIQ-01…04 | **done** |
| `S2-persistencia` | — | pendiente |
| `S3-api-publica` | — | pendiente |
| `S5-clientes` | contratos 4, 5, 6, 7, 8 | pendiente |

Ejecución no tiene `S4`: no publica endpoints de integración.

---

## Cierre del sistema

| Hito | Criterio | Estado |
|---|---|---|
| Reactor verde | `./mvnw clean verify` | **done** |
| Los siete arrancan | `./scripts/smoke-test.sh` | pendiente |
| 48 invariantes cubiertas | Todas con prueba en verde | **done** |
| 11 contratos implementados | Con prueba de cliente y de proveedor | pendiente |
| Flujo vertical | Orden → viaje → ejecución → factura → cobranza, de extremo a extremo | pendiente |

## Progreso

Los contadores se actualizan **sólo en `main`**, al integrar cada rama. Una rama de slice marca `done`
su propia fila y no toca esta tabla: si dos ramas incrementan el mismo número, el merge choca siempre.


| | Hecho | Total |
|---|---:|---:|
| Slices | 10 | 32 |
| Invariantes cubiertas | **48** | 48 |
| Contratos implementados | 0 | 11 |
| Servicios terminados | 0 | 7 |
