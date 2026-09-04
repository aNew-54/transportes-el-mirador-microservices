# ADR 0005: versionado del esquema con Flyway

- Estado: Aceptada
- Fecha: 2026-09-04

## Contexto

El Sprint 0 dejó `spring.jpa.hibernate.ddl-auto=update` en los siete servicios y ninguna herramienta de
migración. `update` no borra ni modifica columnas: acumula. Con cinco personas tocando siete esquemas en
paralelo, las bases divergen y ninguna se puede reproducir desde cero.

El despliegue local ya prometía que un cambio de esquema «debe aplicarse mediante una migración explícita»,
pero no existía el mecanismo para hacerlo.

## Decisión

Cada módulo versiona su propio esquema con Flyway en `src/main/resources/db/migration`, con nombres
`V<n>__<descripcion>.sql`. Hibernate pasa a `ddl-auto=validate`: valida el mapeo, nunca genera tablas.

Se activa `baseline-on-migrate` para las bases de desarrollo que ya tengan tablas creadas por el `update`
anterior.

Ninguna migración cruza esquemas. Un módulo sólo migra el suyo.

## Consecuencias

- Una entidad sin su migración rompe el build. Es deliberado: obliga a que el esquema sea explícito.
- El esquema de cualquier contexto se reproduce desde cero en cualquier máquina.
- Una migración ya aplicada no se edita; se corrige con una migración nueva.
- El primer slice de persistencia de cada servicio debe incluir su `V1`.
