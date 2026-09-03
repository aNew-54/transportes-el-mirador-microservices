# ADR 0003: base de datos por microservicio

- Estado: Aceptada
- Fecha: 2026-09-02

## Contexto

Cada bounded context debe controlar sus invariantes y evolucionar su persistencia sin depender del esquema de otro contexto.

## Decisión

Asignar a cada microservicio un esquema y un usuario MySQL exclusivos. Se prohíben claves foráneas, relaciones JPA, consultas y escrituras entre esquemas.

En desarrollo, los siete esquemas comparten una instancia MySQL creada por Docker Compose. Esta optimización local no cambia la propiedad lógica.

## Consecuencias

- Las referencias externas se almacenan mediante identificadores.
- Los servicios obtienen información externa mediante contratos HTTP.
- No existen transacciones ACID entre microservicios.
- Los procesos distribuidos deberán manejar fallos parciales, reintentos o compensaciones.
