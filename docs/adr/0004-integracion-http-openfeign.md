# ADR 0004: integración HTTP inicial con OpenFeign

- Estado: Aceptada
- Fecha: 2026-09-02

## Contexto

El proyecto de referencia del curso utiliza comunicación HTTP síncrona mediante OpenFeign. El Sprint 0 debe establecer una base comprensible antes de introducir infraestructura de mensajería.

## Decisión

Usar OpenFeign para las primeras integraciones entre microservicios. Las direcciones y tiempos de espera se reciben mediante configuración. Cada consumidor define DTO propios para el contrato que necesita.

No se incorporan todavía API Gateway, Service Discovery, Config Server ni broker de eventos.

## Consecuencias

- Los flujos iniciales son fáciles de ejecutar y depurar.
- Una dependencia caída puede interrumpir una operación síncrona.
- Deben definirse timeouts y traducción explícita de errores.
- Los reportes y notificaciones podrán migrar a eventos cuando exista un flujo vertical estable.
