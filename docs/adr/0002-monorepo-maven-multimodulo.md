# ADR 0002: monorepo Maven multi-módulo

- Estado: Aceptada
- Fecha: 2026-09-02

## Contexto

El equipo necesita desarrollar siete aplicaciones con versiones compatibles de Java, Spring Boot y Spring Cloud, manteniendo una integración sencilla para el trabajo académico.

## Decisión

Conservar las siete aplicaciones en un solo repositorio Maven. Un POM padre agrega los módulos y centraliza las versiones. Cada módulo mantiene su propio POM, clase de arranque, configuración, código y pruebas.

No habrá dependencias Maven entre los módulos de negocio.

## Consecuencias

- Un solo comando verifica el proyecto completo.
- Las actualizaciones técnicas permanecen alineadas.
- Los cambios contractuales pueden revisarse en un mismo pull request.
- El monorepo no autoriza acceso directo entre modelos ni bases.
