# Contratos de API

## Estado

El scaffold del Sprint 0 expone únicamente los endpoints de salud e información proporcionados por Spring Boot Actuator. Las API de negocio todavía no están implementadas.

Los contratos OpenAPI se añadirán en `docs/api/openapi/msvc-<contexto>.yaml` junto con el primer endpoint real de cada servicio. No se crearán especificaciones vacías.

## Convenciones

- API pública bajo `/api/v1`.
- Operaciones internas bajo `/internal/v1` cuando no deban consumirse desde clientes externos.
- JSON como formato de intercambio.
- Fechas y horas en ISO 8601, conservando zona u offset cuando corresponda.
- Importes con monto decimal y código de moneda.
- Errores HTTP con Problem Details (`application/problem+json`).
- DTO separados para solicitud y respuesta.
- Identificadores externos como valores escalares, sin serializar entidades JPA.
- Cambios incompatibles requieren una nueva versión del contrato.

## Matriz inicial de contratos

| Consumidor | Proveedor | Contrato previsto |
|---|---|---|
| Programación | Comercial | Consultar orden de servicio confirmada |
| Programación | Unidades | Consultar elegibilidad de una unidad |
| Programación | Conductores | Consultar elegibilidad de un conductor |
| Ejecución | Programación | Consultar hoja de ruta programada |
| Ejecución | Unidades | Reportar kilometraje y falla mecánica |
| Ejecución | Conductores | Reportar horas conducidas e incidencia |
| Ejecución | Comercial | Reportar diferencia de carga o espera |
| Ejecución | Facturación | Registrar conformidad y conceptos facturables |
| Facturación | Comercial | Consultar snapshot comercial de la orden |
| Facturación | Cobranza | Crear cuenta por cobrar desde una factura |
| Comercial | Cobranza | Consultar estado crediticio |

El detalle de estos once contratos —rutas, esquemas de petición y respuesta, códigos HTTP, idempotencia y
comportamiento ante indisponibilidad— está en [`contracts.md`](contracts.md). Los `.yaml` de OpenAPI se
derivan de ahí al implementar cada endpoint.

## Criterio de aceptación de un contrato

Un contrato está listo cuando tiene esquema OpenAPI, ejemplos válidos, estados HTTP documentados, validaciones, tratamiento de indisponibilidad y una prueba del consumidor o del cliente Feign.
