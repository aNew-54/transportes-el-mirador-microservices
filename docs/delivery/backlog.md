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
| `S3-api-publica` | — | **done** |
| `S4-api-interna` | contratos 2 y 5 | **done** |

### msvc-conductores · revisa Brayam Alfaro

| Slice | Invariantes | Estado |
|---|---|---|
| `S1-dominio` | CON-01, CON-02, CON-03 | **done** |
| `S2-persistencia` | — | **done** |
| `S3-api-publica` | — | **done** |
| `S4-api-interna` | contratos 3 y 6 | **done** |

### msvc-cobranza · revisa María Belén Vilca

| Slice | Invariantes | Estado |
|---|---|---|
| `S1-dominio` | CCC-01, CCC-02, CCC-03, PAG-01, PAG-02 | **done** |
| `S2-persistencia` | — | **done** |
| `S3-api-publica` | — | **done** |
| `S4-api-interna` | contratos 10 y 11 | **done** |

---

## Ola 2 — Comercial

### msvc-comercial · revisa Sarah Herrera

| Slice | Invariantes | Estado |
|---|---|---|
| `S1-dominio` | CLI-01, COT-01, COT-02, ORD-01, ORD-02, CTM-01, CTM-02, TAR-01 | **done** |
| `S2-persistencia` | — | **done** |
| `S3-api-publica` | — | **done** |
| `S4-api-interna` | contratos 1, 7 y 9 | **done** |
| `S5-clientes` | contrato 11 → Cobranza | **done** |

---

## Ola 3 — Facturación

### msvc-facturacion · revisa María Belén Vilca

| Slice | Invariantes | Estado |
|---|---|---|
| `S1-dominio` | FAC-01, FAC-02, FAC-03, FAC-04, FAC-05, NCR-01 | **done** |
| `S2-persistencia` | — | **done** |
| `S3-api-publica` | — | **done** |
| `S4-api-interna` | contrato 8 | **done** |
| `S5-clientes` | contratos 9 → Comercial, 10 → Cobranza | **done** |

---

## Ola 4 — Programación (Core Domain)

### msvc-programacion · revisa Brayam Alfaro

Único servicio con un slice partido en dos: la consolidación no se delega.

| Slice | Invariantes | Quién | Estado |
|---|---|---|---|
| `S1a-dominio-base` | AGU-01, AGU-02, AGC-01, AGC-02, VIA-01, VIA-07 | agy `flash` | **done** |
| `S1b-consolidacion` | VIA-02, VIA-03, VIA-04, VIA-05, VIA-06 | **Claude** | **done** |
| `S2-persistencia` | — | agy `flash` | **done** |
| `S3-api-publica` | — | agy `flash` | **done** |
| `S4-api-interna` | contrato 4 | agy `flash` | **done** |
| `S5-clientes` | contratos 1, 2, 3 | agy `pro` + Claude | **done** |

---

## Ola 5 — Ejecución

### msvc-ejecucion · revisa Alexander Infante

| Slice | Invariantes | Estado |
|---|---|---|
| `S1-dominio` | EJV-01…05, LIQ-01…04 | **done** |
| `S2-persistencia` | — | **done** |
| `S3-api-publica` | — | **done** |
| `S5-clientes` | contratos 4, 5, 6, 7, 8 | **done** |
| `S6-cierre` | LIQ-04, EJV-04, UNI-03, CON-02 | **done** |

Ejecución no tiene `S4`: no publica endpoints de integración. Es el único servicio con `S6`: es el
único que empuja hechos a otros cuatro contextos al terminar, y ese empuje resultó ser un slice.

---

## Cierre del sistema

| Hito | Criterio | Estado |
|---|---|---|
| Reactor verde | `./mvnw clean verify` | **done** |
| Los siete arrancan | `./scripts/smoke-test.sh` | **done** |
| 48 invariantes cubiertas | Todas con prueba en verde | **done** |
| 11 contratos implementados | Con prueba de cliente y de proveedor | **done** |
| Contratos cableados | Un servicio de aplicación llama a cada cliente | **10 de 11** |
| Flujo vertical | Orden → viaje → ejecución → factura → cobranza, de extremo a extremo | **done** |

## Progreso

Los contadores se actualizan **sólo en `main`**, al integrar cada rama. Una rama de slice marca `done`
su propia fila y no toca esta tabla: si dos ramas incrementan el mismo número, el merge choca siempre.


| | Hecho | Total | |
|---|---:|---:|---|
| Slices | **33** | 33 | **100 %** |
| Invariantes cubiertas | **48** | 48 | 100 % |
| Contratos con proveedor listo | **11** | 11 | 100 % |
| Contratos con cliente Feign | **11** | 11 | **100 %** |
| Contratos que alguien llama | **10** | 11 | **91 %** |
| Servicios terminados | **7** | 7 | **100 %** |

Un servicio esta terminado cuando no le queda ningun slice. Unidades, Conductores y Cobranza lo estan:
la regla 10 no les da flecha saliente, asi que su ultimo slice es `S4` y no tienen `S5` que esperar.
Los cuatro restantes tambien lo estan: Ejecucion cerro el ultimo con `S6`.

Los 33 slices estan integrados y los once contratos tienen cliente y proveedor, cada uno con su prueba
de gateway y su prueba de stub contra el JSON de `contracts.md`.

**Lo que queda, dicho en voz alta.** De los once contratos, diez los llama un servicio de
aplicacion. El que falta es la diferencia de carga del contrato 7: Ejecucion no tiene en ningun sitio
del agregado lo declarado ni lo real, asi que cablearla exige una entidad `DiferenciaDeCarga` por
parada, con su migracion y sus invariantes. Es un slice propio, no un apendice del cierre.

| Contrato | Consumidor | ¿Lo llama alguien? |
|---|---|---|
| 1, 2, 3 | Programación | sí — `ViajeService` |
| 4 | Ejecución | sí — `EjecucionDeViajeService.crear` |
| 5, 6, 8 | Ejecución | sí — `EjecucionDeViajeService.cerrar` |
| 7 · esperas | Ejecución | sí — `EjecucionDeViajeService.cerrar` |
| 7 · diferencia de carga | Ejecución | **no** — le falta el concepto de dominio |
| 9, 10 | Facturación | sí — `FacturaService` |
| 11 | Comercial | sí — `OrdenDeServicioService` |

**Los tres defectos de la misma familia.** Tres invariantes se comprobaban contra un dato que ponia
quien llamaba, y las tres eran infalsificables por eso. Aparecieron una por slice y ninguna la vio una
suite en verde:

| Invariante | Dato que venia en el cuerpo | Slice que lo cerro |
|---|---|---|
| VIA-04 | La clausula del contrato marco | `S5` de Programación, con el contrato 1 |
| ORD-02 | El estado crediticio del cliente | `S5` de Comercial, con el contrato 11 |
| LIQ-04 | Si quedaban liquidaciones pendientes | `S6` de Ejecución, **sin contrato**: el dato era suyo |

El tercero es el que mas dice. No hacia falta ningun contrato para arreglarlo: las liquidaciones son
del propio contexto y el metodo de repositorio que las cuenta existia desde `S2`. Nadie lo llamaba.

**Los seis defectos que solo se vieron encadenando los contextos.** `scripts/flujo-vertical.sh`
recorre una orden a credito desde el alta del cliente hasta la cuenta por cobrar: 23 pasos, siete
contextos, once contratos. Necesito nueve pasadas para llegar al final, y cada parada era un defecto
real de produccion sobre una suite en verde.

| # | Que estaba roto | Consecuencia en produccion |
|---|---|---|
| 1 | `CuentaCorrienteDelCliente` no la construia nadie | Ningun cliente podia pedir a credito ni entrar al ledger |
| 2 | El id derivado del documento no cabia en su columna | Ninguna unidad podia tener SOAT, luego ninguna era elegible |
| 3 | Feign mandaba las fechas con el locale del JVM | Los contratos 2 y 3 respondian 400 a toda consulta |
| 4 | Los gateways tiraban el cuerpo del fallo remoto | Todo fallo de integracion era indiagnosticable |
| 5 | Ejecucion no exponia `marcarEntregada` | Ninguna ejecucion podia cerrarse nunca |
| 6 | `detraccion.cuentaBancaria` era obligatoria | Ninguna factura sin detraccion entraba a la cartera |

Ninguno se ve con una prueba de modulo. Cinco de los seis estaban en el camino entre dos contextos, y
el sexto —el id del documento— solo aparece cuando el identificador lo genera produccion y no la
prueba. Los tres primeros bastaban, cada uno por su cuenta, para que el sistema no sirviera para nada.

**El defecto que ninguna prueba de modulo podia ver.** Al encadenar la orden con la factura salio que
`CuentaCorrienteDelCliente` no la construia nadie en produccion: solo la fabricaban a mano seis
ficheros de prueba. Los dos contratos que Cobranza publica resolvian la cuenta con un `orElseThrow`,
asi que el 10 respondia `404` a toda factura y el 11 a toda consulta de credito. **Ningun cliente
podia pedir una orden a credito ni entrar al ledger**, y las 109 pruebas del modulo seguian en verde.

Es el unico defecto del proyecto que no se ve dentro de un modulo ni en un contrato aislado. Una
prueba de stub responde lo que el contrato dice; una prueba de dominio parte del estado que le
convenga. Ninguna de las dos pregunta quien crea el primer objeto. Solo recorrer el camino entero lo
pregunta, y por eso el hito de flujo vertical no era un adorno.
