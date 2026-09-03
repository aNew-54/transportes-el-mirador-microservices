# Guía de contribución

## Flujo de trabajo

1. Actualizar `main` y crear una rama corta para una sola tarea.
2. Implementar el cambio únicamente dentro del alcance de la tarea.
3. Ejecutar `./mvnw clean verify` desde la raíz.
4. Crear un pull request hacia `main`.
5. Solicitar la revisión de al menos otro integrante.
6. Integrar únicamente cuando las pruebas y la revisión estén aprobadas.

## Nombres de ramas

```text
feat/<servicio>/<descripcion>
fix/<servicio>/<descripcion>
test/<servicio>/<descripcion>
docs/<descripcion>
chore/<descripcion>
```

Ejemplo: `feat/programacion/reservar-unidad`.

## Commits

El proyecto utiliza Conventional Commits:

```text
feat(programacion): implementa reserva de unidad
fix(cobranza): corrige cálculo de días de atraso
test(ejecucion): agrega prueba del checklist
docs(architecture): documenta propiedad de datos
```

## Reglas de arquitectura

- Cada microservicio corresponde a un único bounded context.
- Cada servicio modifica exclusivamente su propia base de datos.
- No se comparten entidades JPA, repositorios ni modelos de dominio entre servicios.
- La comunicación entre servicios utiliza DTO definidos en la frontera HTTP.
- Los controladores delegan la lógica a los servicios de aplicación.
- Las credenciales y direcciones externas se reciben mediante configuración.

## Definición de terminado

Un cambio está terminado cuando compila con Java 26, incluye las pruebas pertinentes, conserva la compatibilidad de sus contratos, no contiene secretos y actualiza la documentación afectada.
