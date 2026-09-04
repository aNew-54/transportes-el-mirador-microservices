# ADR 0006: verificación de persistencia con Testcontainers

- Estado: Aceptada
- Fecha: 2026-09-04

## Contexto

Las pruebas de arranque de contexto excluyen `DataSourceAutoConfiguration` y `HibernateJpaAutoConfiguration`
para que el reactor se verifique sin depender de Docker. La integración continua ejecutaba únicamente esas
pruebas.

El efecto es que el mapeo JPA y las migraciones no se verifican en ninguna parte. En cuanto existan entidades,
la integración continua daría verde sobre un esquema inválido y el fallo aparecería al arrancar la aplicación.

## Decisión

Cada módulo incorpora una prueba `Persistencia<Contexto>IT` que se ejecuta con Failsafe en la fase `verify`,
contra un MySQL real levantado por Testcontainers. La prueba exige que Flyway haya migrado el esquema y que
Hibernate lo valide.

Surefire conserva la prueba de contexto sin base de datos. `./mvnw test` sigue sin necesitar Docker.

El slice `@DataJpaTest` importa sólo Hibernate y los repositorios, de modo que la autoconfiguración de Flyway
se declara de forma explícita en la prueba.

## Consecuencias

- `./mvnw verify` exige Docker; `./mvnw test` no.
- La integración continua tarda unos dos minutos más y no necesita un servicio `mysql:` en el job: los
  runners de GitHub ya traen Docker.
- Una entidad mal mapeada falla en la integración continua, no al arrancar en la máquina de alguien.
