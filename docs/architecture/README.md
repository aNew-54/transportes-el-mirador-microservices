# Arquitectura

## Propósito

El sistema cubre la gestión del transporte terrestre de carga general de Transportes El Mirador S.A.C., desde la cotización hasta la cobranza. La optimización de viajes, consolidación de cargas y asignación de recursos constituye el Core Domain.

## Límites

La solución utiliza siete bounded contexts y conserva una aplicación Spring Boot por cada uno:

| Servicio | Bounded context | Puerto | Esquema MySQL |
|---|---|---:|---|
| `msvc-comercial` | Gestión Comercial | 8010 | `mirador_comercial` |
| `msvc-programacion` | Programación y Despacho | 8020 | `mirador_programacion` |
| `msvc-ejecucion` | Ejecución y Seguimiento | 8030 | `mirador_ejecucion` |
| `msvc-unidades` | Gestión de Unidades | 8040 | `mirador_unidades` |
| `msvc-conductores` | Gestión de Conductores | 8050 | `mirador_conductores` |
| `msvc-facturacion` | Facturación | 8060 | `mirador_facturacion` |
| `msvc-cobranza` | Cobranza | 8070 | `mirador_cobranza` |

## Reglas de dependencia

1. Un módulo Maven produce una aplicación desplegable y corresponde a un solo bounded context.
2. Cada servicio es dueño exclusivo de su modelo y sus tablas.
3. Los demás servicios conservan únicamente identificadores externos y DTO de integración.
4. No existen dependencias Maven entre módulos de negocio.
5. Las transacciones terminan en la frontera de la base local.
6. Los fallos remotos deben traducirse a errores de integración explícitos; no se interpretan todos como `404`.
7. Los contratos HTTP deben versionarse y documentarse antes de ser consumidos.

## Flujo vertical inicial

1. Comercial confirma una orden de servicio.
2. Programación consulta la orden, verifica una unidad y un conductor y programa el viaje.
3. Ejecución obtiene la hoja de ruta, aprueba el checklist y registra la entrega.
4. Facturación obtiene los datos inmutables de la orden y emite una factura.
5. Cobranza crea la cuenta por cobrar y aplica el pago.

Una unidad o conductor puede cambiar durante la ejecución sin cambiar la identidad del viaje. Un viaje puede transportar varias órdenes, pero cada orden conserva su conformidad y factura.

## Lecturas relacionadas

- [Mapa de integraciones](context-map.md)
- [Despliegue local](deployment.md)
- [Registro de decisiones](../adr/README.md)
- [Convenciones de API](../api/README.md)
