# Registro de invariantes

Las 48 invariantes del diseño táctico, con código estable. **Este es el criterio de éxito del proyecto**: cada
invariante debe tener al menos una prueba que la viole y espere el fallo. Una invariante sin prueba en rojo
antes de implementarla no cuenta como cubierta.

El código no cambia nunca, aunque se reordene la tabla. Se cita desde las specs, los prompts de delegación y
los mensajes de commit.

| Servicio | Invariantes | Agregados |
|---|---:|---:|
| `msvc-comercial` | 8 | 5 |
| `msvc-programacion` | 11 | 3 |
| `msvc-ejecucion` | 9 | 2 |
| `msvc-unidades` | 6 | 3 |
| `msvc-conductores` | 3 | 1 |
| `msvc-facturacion` | 6 | 2 |
| `msvc-cobranza` | 5 | 2 |
| **Total** | **48** | **18** |

---

## msvc-programacion — Programación y Despacho (Core Domain)

### Agregado `Viaje` (VIA)

| Código | Invariante |
|---|---|
| VIA-01 | Un viaje no pasa a `Programado` sin unidad y al menos un conductor asignados. |
| VIA-02 | El peso y el volumen totales de la carga consolidada no pueden exceder la capacidad de la unidad. |
| VIA-03 | Todas las órdenes consolidadas deben pertenecer al mismo corredor y a ventanas de fechas compatibles. |
| VIA-04 | No se consolida una orden cuyo contrato marco lo prohíbe. |
| VIA-05 | Las cargas consolidadas deben ser físicamente compatibles entre sí. |
| VIA-06 | La secuencia de paradas es tal que la carga que se descarga primero se estiba al final. |
| VIA-07 | Un viaje despachado no admite nuevas órdenes. |

### Agregado `AgendaDeUnidad` (AGU)

| Código | Invariante |
|---|---|
| AGU-01 | Dos reservas vigentes de la misma unidad no pueden tener ventanas de tiempo solapadas. |
| AGU-02 | No se otorga reserva sobre una unidad inoperativa o con mantenimiento preventivo vencido. |

### Agregado `AgendaDeConductor` (AGC)

| Código | Invariante |
|---|---|
| AGC-01 | Dos reservas vigentes del mismo conductor no pueden tener ventanas de tiempo solapadas. |
| AGC-02 | No se otorga reserva sobre un conductor no habilitado. |

---

## msvc-ejecucion — Ejecución y Seguimiento

### Agregado `EjecuciónDeViaje` (EJV)

| Código | Invariante |
|---|---|
| EJV-01 | La ejecución no se inicia sin check-list de salida aprobado. |
| EJV-02 | Cada parada de descarga recoge una conformidad; existe una por cada orden de servicio del viaje. |
| EJV-03 | La ejecución sólo pasa a `Entregada` cuando todas sus conformidades están firmadas. |
| EJV-04 | Una ejecución entregada no admite nuevos hitos ni la reapertura de paradas atendidas. |
| EJV-05 | Un transbordo cambia la unidad ejecutora sin crear una nueva ejecución: se conserva la identidad del viaje. |

### Agregado `LiquidaciónDeViaje` (LIQ)

| Código | Invariante |
|---|---|
| LIQ-01 | Todo gasto rendido debe contar con comprobante. |
| LIQ-02 | El saldo se calcula como anticipo menos gastos; nunca se almacena. |
| LIQ-03 | Una liquidación aprobada es inmutable. |
| LIQ-04 | La ejecución del viaje no se cierra mientras exista una liquidación pendiente. |

---

## msvc-comercial — Gestión Comercial

### Agregado `Cliente` (CLI)

| Código | Invariante |
|---|---|
| CLI-01 | Un cliente con crédito suspendido no puede contratar a crédito, pero sí al contado. |

### Agregado `Cotización` (COT)

| Código | Invariante |
|---|---|
| COT-01 | Una cotización vencida no puede aceptarse, sólo recotizarse. |
| COT-02 | Una tarifa por debajo del tarifario exige autorización de gerencia registrada. |

### Agregado `OrdenDeServicio` (ORD)

| Código | Invariante |
|---|---|
| ORD-01 | Una orden ya programada no admite cambio de carga sin generar reajuste. |
| ORD-02 | La condición de pago debe ser consistente con el estado crediticio vigente. |

### Agregado `ContratoMarco` (CTM)

| Código | Invariante |
|---|---|
| CTM-01 | Las tarifas pactadas sólo aplican dentro del periodo de vigencia. |
| CTM-02 | La cláusula de consolidación obliga a todas las órdenes del contrato. |

### Agregado `Tarifario` (TAR)

| Código | Invariante |
|---|---|
| TAR-01 | Sólo un tarifario puede estar vigente a la vez. |

---

## msvc-unidades — Gestión de Unidades

### Agregado `Unidad` (UNI)

| Código | Invariante |
|---|---|
| UNI-01 | Una unidad con cualquiera de sus cuatro documentos vencido pasa automáticamente a inoperativa. |
| UNI-02 | Una unidad con mantenimiento preventivo vencido no puede habilitarse. |
| UNI-03 | El kilometraje nunca decrece. |

### Agregado `OrdenDeMantenimiento` (OMT)

| Código | Invariante |
|---|---|
| OMT-01 | Una orden cerrada es inmutable. |
| OMT-02 | El kilometraje registrado no puede ser menor al del último mantenimiento de la unidad. |

### Agregado `Repuesto` (REP)

| Código | Invariante |
|---|---|
| REP-01 | Las existencias nunca pueden ser negativas. |

---

## msvc-conductores — Gestión de Conductores

### Agregado `Conductor` (CON)

| Código | Invariante |
|---|---|
| CON-01 | Un conductor con licencia vencida o categoría insuficiente no está habilitado. |
| CON-02 | Las horas acumuladas no pueden superar el máximo normado. |
| CON-03 | Sin inducción vigente no está habilitado para clientes que la exigen. |

---

## msvc-facturacion — Facturación

### Agregado `Factura` (FAC)

| Código | Invariante |
|---|---|
| FAC-01 | Sólo se emite con conformidad registrada, o con falso flete en cancelaciones posteriores al despacho. |
| FAC-02 | Una factura corresponde a exactamente una orden de servicio. |
| FAC-03 | Una factura emitida es inmutable; se corrige mediante nota de crédito. |
| FAC-04 | Monto neto más detracción debe igualar el total. |
| FAC-05 | Una incidencia de daño, faltante o rechazo sin resolver bloquea la emisión. |

### Agregado `NotaDeCrédito` (NCR)

| Código | Invariante |
|---|---|
| NCR-01 | El monto no puede exceder el saldo de la factura que ajusta. |

---

## msvc-cobranza — Cobranza

### Agregado `CuentaCorrienteDelCliente` (CCC)

| Código | Invariante |
|---|---|
| CCC-01 | El crédito se suspende automáticamente al existir una cuenta con más de treinta días de atraso. |
| CCC-02 | El saldo de una cuenta por cobrar nunca es negativo. |
| CCC-03 | Una factura no se cancela mientras falte el pago del cliente o el depósito de detracción. |

### Agregado `Pago` (PAG)

| Código | Invariante |
|---|---|
| PAG-01 | La suma de las aplicaciones no puede exceder el monto del pago. |
| PAG-02 | Un pago no puede aplicarse a cuentas de un cliente distinto. |

---

## Invariantes que cruzan contextos

Cuatro invariantes dependen de datos que el servicio no posee. **No se resuelven con una consulta JPA**: el
servicio consulta el contrato HTTP del proveedor y, si el proveedor no responde, la operación falla de forma
explícita. Nunca se asume el caso favorable.

| Código | Depende de | Contrato |
|---|---|---|
| AGU-02 | `msvc-unidades` | `GET /internal/v1/unidades/{id}/elegibilidad` |
| AGC-02 | `msvc-conductores` | `GET /internal/v1/conductores/{id}/elegibilidad` |
| VIA-04 | `msvc-comercial` | `GET /internal/v1/ordenes/{id}` (cláusula de consolidación) |
| ORD-02 | `msvc-cobranza` | `GET /internal/v1/clientes/{id}/estado-crediticio` |

FAC-01 y FAC-05 dependen de datos que **Ejecución empuja** hacia Facturación, no que Facturación consulte:
la conformidad llega por `POST /internal/v1/conformidades`. Facturación no puede emitir sin haberla recibido.
