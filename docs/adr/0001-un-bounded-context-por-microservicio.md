# ADR 0001: un bounded context por microservicio

- Estado: Aceptada
- Fecha: 2026-09-02

## Contexto

El diseño estratégico identifica siete contextos con lenguajes, reglas y propietarios de datos diferentes. Agruparlos en menos aplicaciones debilitaría las fronteras definidas en el modelo.

## Decisión

Implementar siete aplicaciones independientes, una por cada bounded context: Comercial, Programación y Despacho, Ejecución y Seguimiento, Unidades, Conductores, Facturación y Cobranza.

Una persona puede liderar más de una aplicación. La distribución del equipo no modifica las fronteras del dominio.

## Consecuencias

- Los despliegues y las bases se pueden evolucionar por contexto.
- Aumenta la cantidad de configuración y contratos de integración.
- Un cambio que atraviesa contextos requiere coordinación entre servicios.
- Se prohíbe crear un módulo de dominio común que vuelva a acoplarlos.
