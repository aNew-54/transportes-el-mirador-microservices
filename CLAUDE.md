# Contrato de orquestación — Transportes El Mirador

Monorepo Maven con 7 microservicios Spring Boot (Java 26, Spring Boot 4.1.0, Spring Cloud 2025.1.2, MySQL 8.4).
Un bounded context por microservicio. Sin dependencias Maven entre módulos de negocio. Sin FK ni entidades JPA compartidas.

## Reparto de roles

**Claude es el orquestador. Nunca es el programador por defecto.**

| Claude (orquestador) | `antigravity-delegate` (agy / Gemini) |
|---|---|
| Lee el dominio y decide el diseño | No decide diseño; ejecuta una spec escrita |
| Escribe las specs y los contratos HTTP | Escribe entidades, VO, repositorios, mappers, DTO, controllers, clientes Feign |
| Define los criterios de éxito | Escribe las pruebas que esos criterios exigen |
| Revisa el diff y corre los gates | Devuelve un digest, nunca declara éxito |
| Implementa el «20% duro»: invariantes del Core, consolidación de cargas, secuencia de estiba | Implementa el 80% mecánico |
| Decide qué se integra | No integra nada |

Claude escribe código de producción sólo cuando: (a) es lógica del Core Domain con invariantes acopladas,
(b) agy falló dos veces sobre la misma spec, o (c) el cambio es menor a ~30 líneas en 1–2 archivos.

## Fuentes de verdad

| Qué | Dónde |
|---|---|
| Dominio narrativo, subdominios, mapa de contexto | `docs/Tarea - Diseño estratégico de Transportes de carga.pdf` |
| Agregados, entidades, objetos de valor, invariantes | `docs/Tarea - Diseño Táctico - Transportes El Mirador.pdf` |
| Método de trabajo y Definition of Done | `docs/delivery/README.md` |
| Las 48 invariantes con código y test asignado | `docs/delivery/invariantes.md` |
| Spec ejecutable de cada servicio | `docs/delivery/servicios/msvc-<contexto>.md` |
| Los 11 contratos de integración | `docs/api/contracts.md` |
| Orden de trabajo | `docs/delivery/backlog.md` |

Un desacuerdo entre el código y estos documentos es un defecto del código, salvo que un ADR nuevo diga lo contrario.

## Reglas duras (no negociables)

1. Ningún módulo importa `pe.edu.unc.elmirador.<otro-contexto>`. Verificable y verificado en cada gate.
2. Ninguna entidad JPA cruza la frontera HTTP. Sólo DTO de request/response.
3. Las referencias a otros contextos son identificadores escalares, nunca objetos ni relaciones JPA.
4. API pública en `/api/v1`. Integración entre servicios en `/internal/v1`.
5. Errores con `application/problem+json` (RFC 7807). Nunca un `404` genérico para un fallo de integración.
6. Fechas ISO 8601 con offset. Importes con monto y código de moneda.
7. Todo cliente Feign declara timeout y traduce el fallo remoto a una excepción de integración propia.
8. No se añaden dependencias Maven sin decisión explícita de Claude.
9. Cada invariante del diseño táctico tiene al menos una prueba que la viola y espera el fallo.
10. Un módulo lleva OpenFeign **sólo** si el mapa de contexto le da una flecha saliente. Unidades,
    Conductores y Cobranza son proveedores puros: sin la dependencia y sin `@EnableFeignClients`.
11. El esquema lo crea Flyway, nunca Hibernate. Con `ddl-auto=validate`, una entidad sin su migración
    rompe el build. Es deliberado.
12. Los objetos de valor viven en `models/vo` como `@Embeddable`, sin `@Id`. Su lógica vive con ellos.
13. **Los identificadores Java son ASCII.** La documentación conserva la tilde; el código, no:
    `estaHabilitada()`, `estaVigente()`, `kmUltimoServicio`, `tramoDeGestion()`, `Induccion`,
    `NotaDeCredito`. macOS almacena los nombres de archivo en NFD y git en NFC: un
    `Inducción.java` se corrompe al clonar en Linux. Cada spec lleva la tabla de correspondencia.

## Ciclo de trabajo por slice

```
1. Claude escribe/actualiza la spec del slice        → docs/delivery/servicios/*.md
2. Claude crea la rama                                → feat/<msvc>/<slice>
3. Claude delega a agy apuntando a la spec            → subagente antigravity-delegate
4. agy escribe código y pruebas
5. Claude VERIFICA (nunca confía en el auto-reporte):
     git status                                       → ¿escribió de verdad?
     ./mvnw -pl <msvc> verify                          → ¿compila y pasa?
     grep de aislamiento                               → ¿respetó la frontera?
     revisión del diff contra las invariantes
6. Falla → Claude corrige o re-delega con feedback puntual. No se re-delega la spec entera.
7. Pasa   → Claude marca el slice como Done en docs/delivery/backlog.md
```

`--yolo` no se usa. agy tiene permisos scoped en `~/.gemini/antigravity-cli/settings.json`.

## Comandos del gate

`verify` necesita Docker: Failsafe levanta un MySQL real con Testcontainers para las pruebas `*IT`.

```bash
./mvnw -pl msvc-comercial verify
```

Sin Docker, sólo las pruebas de contexto:

```bash
./mvnw -pl msvc-comercial test
```

```bash
./mvnw clean verify
```

```bash
docker compose up -d && ./scripts/smoke-test.sh
```

Aislamiento entre contextos — debe devolver 0 líneas:

```bash
for m in comercial programacion ejecucion unidades conductores facturacion cobranza; do grep -rn "^import pe\.edu\.unc\.elmirador\." "msvc-$m/src" 2>/dev/null | grep -v "elmirador\.$m\."; done
```

## Convenciones de código

- Paquete raíz `pe.edu.unc.elmirador.<contexto>`.
- Subpaquetes ya creados: `models/entity`, `models/vo`, `repositories`, `services`, `controllers`,
  `dto/request`, `dto/response`, `mappers`, `clients`, `config`, `exceptions`.
- **`models/entity`**: raíces de agregado y entidades hijas, como entidades JPA ricas.
  **`models/vo`**: objetos de valor, `record` o clase inmutable con `@Embeddable`, sin `@Id`.
  **`services`**: servicio de aplicación. Orquesta y transacciona; no decide reglas de negocio.
- Un `if` de negocio dentro de un `service` es un defecto: esa regla pertenece al VO o al agregado.
  Si aparece en dos servicios, ya se duplicó.
- Cada migración en `src/main/resources/db/migration/V<n>__<descripcion>.sql`, en el módulo dueño.
- Los métodos de negocio del diseño táctico (`total()`, `cabeEn()`, `seSolapaCon()`, `esCompatibleCon()`, `secuenciaDeEstiba()`, `tramoDeGestión()`, `montoNeto()`) se implementan con ese nombre, en el VO o agregado que el PDF indica.
- Pruebas con JUnit 5 y `@SpringBootTest` sólo cuando hace falta contexto; el resto son pruebas de dominio puras.
