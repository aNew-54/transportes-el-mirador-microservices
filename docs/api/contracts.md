# Contratos de integración

Los once contratos autorizados por el [mapa de contexto](../architecture/context-map.md). Ninguna otra
llamada entre servicios está permitida.

## Cómo leer esta tabla

- **Consumidor** es quien hace la llamada HTTP y declara el cliente Feign.
- **Proveedor** es quien publica el endpoint bajo `/internal/v1`.
- La dirección DDD (upstream/downstream) no siempre coincide con la dirección HTTP. En los contratos 5 a 8,
  Ejecución es el *proveedor de la información* en términos de DDD, pero es también quien hace el `POST`:
  empuja el dato hacia quien lo necesita.

| # | Consumidor | Proveedor | Patrón DDD | Operación |
|---|---|---|---|---|
| 1 | Programación | Comercial | Partnership | Consultar orden confirmada |
| 2 | Programación | Unidades | Customer-Supplier | Consultar elegibilidad de unidad |
| 3 | Programación | Conductores | Customer-Supplier | Consultar elegibilidad de conductor |
| 4 | Ejecución | Programación | Conformist | Obtener hoja de ruta |
| 5 | Ejecución | Unidades | Customer-Supplier | Reportar kilometraje y falla |
| 6 | Ejecución | Conductores | Customer-Supplier | Reportar horas e incidencia |
| 7 | Ejecución | Comercial | Customer-Supplier | Reportar diferencia de carga y espera |
| 8 | Ejecución | Facturación | Customer-Supplier | Registrar conformidad y conceptos |
| 9 | Facturación | Comercial | Conformist | Consultar snapshot facturable |
| 10 | Facturación | Cobranza | Conformist | Crear cuenta por cobrar |
| 11 | Comercial | Cobranza | Customer-Supplier | Consultar estado crediticio |

## Reglas comunes

1. Todo endpoint de integración vive bajo `/internal/v1`. No se expone a clientes externos.
2. Peticiones y respuestas en JSON. Fechas ISO 8601 con offset. Importes como `{ "monto": "1250.00", "moneda": "PEN" }`.
3. Los errores usan `application/problem+json` con `type`, `title`, `status`, `detail` e `instance`.
4. Todo cliente Feign hereda `connect-timeout=3000ms` y `read-timeout=5000ms` de `application.properties`.
5. Un fallo remoto se traduce a una excepción de integración propia del consumidor
   (`<Contexto>IntegrationException`). **Nunca se convierte en `404` ni se interpreta como «no existe».**
6. Los `POST` de reporte (contratos 5 a 8) son idempotentes por `Idempotency-Key`. Un reintento con la misma
   clave devuelve `200` con el resultado original, no duplica el efecto. **La cabecera es obligatoria en
   los siete endpoints**, y su formato lo fija esta tabla:

   | Endpoint | Clave |
   |---|---|
   | `POST /unidades/{id}/kilometraje` | `<viajeId>:km-final` |
   | `POST /unidades/{id}/fallas` | `<viajeId>:falla:<fallaId>` |
   | `POST /conductores/{id}/horas-conduccion` | `<viajeId>:<conductorId>:horas` |
   | `POST /conductores/{id}/incidencias` | `<viajeId>:<conductorId>:incidencia:<incidenciaId>` |
   | `POST /ordenes/{id}/diferencias-de-carga` | `<viajeId>:<ordenId>:diferencia` |
   | `POST /ordenes/{id}/esperas` | `<viajeId>:<ordenId>:espera:<punto>` |
   | `POST /conformidades` | `<viajeId>:<ordenId>:conformidad` |

   Las cuatro que llevan un identificador o un punto se anaden aqui: el documento sólo escribia tres,
   y los siete proveedores exigen la cabecera desde `S4`. El criterio es que **la clave nombra el
   hecho, no el momento de enviarlo**. El kilometraje final y la conformidad son únicos por viaje, así
   que el par ya los identifica; una falla o una incidencia pueden repetirse dentro del mismo viaje y
   necesitan el identificador que Ejecución les dio; una espera se repite por punto, y el punto la
   nombra. Una clave con la hora de envío o un `UUID` aleatorio convierte el reintento en un hecho
   nuevo, que es justo lo que la idempotencia existe para impedir.
7. El consumidor sólo persiste el identificador del recurso remoto. Nunca su representación completa como
   entidad propia, salvo los *snapshots* explícitos (contrato 9), que son inmutables por diseño.

---

## 1. Programación → Comercial · orden confirmada

Sostiene **VIA-04** (no consolidar una orden cuyo contrato marco lo prohíbe).

```
GET /internal/v1/ordenes/{ordenId}
```

Respuesta `200`:

```json
{
  "ordenId": "ORD-2026-000123",
  "clienteId": "CLI-0007",
  "estado": "CONFIRMADA",
  "carga": { "pesoKg": 8500, "volumenM3": 24.5, "embalaje": "PALLETS", "naturaleza": "ALIMENTARIA" },
  "ruta": { "origen": "Cajamarca", "destino": "Trujillo", "corredor": "COSTA_NORTE", "distanciaKm": 296 },
  "ventana": { "inicio": "2026-09-10T06:00:00-05:00", "fin": "2026-09-10T18:00:00-05:00" },
  "permiteConsolidacion": true,
  "restriccionesConsolidacion": ["SOLO_CARGA_ALIMENTARIA"],
  "tipoUnidadRequerido": "FURGON"
}
```

| Estado | Cuándo |
|---|---|
| `200` | Orden existe y está confirmada |
| `404` | La orden no existe |
| `409` | La orden existe pero no está confirmada |

`permiteConsolidacion` y `restriccionesConsolidacion` provienen de la `CláusulaDeConsolidación` del contrato
marco. Programación no interpreta el contrato marco: consume la decisión ya resuelta por Comercial.

---

## 2. Programación → Unidades · elegibilidad de unidad

Sostiene **AGU-02** y alimenta **VIA-02**.

```
GET /internal/v1/unidades/{unidadId}/elegibilidad?desde={iso}&hasta={iso}&pesoKg={n}&volumenM3={n}&tipoCargaRequerido={t}
```

Respuesta `200`:

```json
{
  "unidadId": "UNI-004",
  "elegible": false,
  "motivos": ["DOCUMENTO_VENCIDO:SOAT", "MANTENIMIENTO_VENCIDO"],
  "capacidad": { "pesoMaximoKg": 10000, "volumenMaximoM3": 32.0 },
  "tipoUnidad": "FURGON",
  "estadoOperativo": "INOPERATIVA"
}
```

Motivos posibles: `DOCUMENTO_VENCIDO:<tipo>`, `MANTENIMIENTO_VENCIDO`, `EN_TALLER`, `INOPERATIVA`,
`CAPACIDAD_INSUFICIENTE`, `TIPO_INCOMPATIBLE`.

`elegible: false` es una respuesta `200` válida, no un error. Programación registra el motivo y busca otra unidad.

---

## 3. Programación → Conductores · elegibilidad de conductor

Sostiene **AGC-02**.

```
GET /internal/v1/conductores/{conductorId}/elegibilidad?desde={iso}&hasta={iso}&tipoUnidad={t}&clienteId={id}
```

Respuesta `200`:

```json
{
  "conductorId": "CON-011",
  "elegible": false,
  "motivos": ["INDUCCION_VENCIDA:CLI-0019", "HORAS_INSUFICIENTES"],
  "categoriaLicencia": "A-IIIB",
  "horasDisponibles": 3.5
}
```

Motivos posibles: `LICENCIA_VENCIDA`, `CATEGORIA_INSUFICIENTE`, `HORAS_INSUFICIENTES`,
`INDUCCION_VENCIDA:<clienteId>`, `NO_HABILITADO`.

`clienteId` es opcional: sólo se envía cuando el destino exige inducción de seguridad (CON-03).

---

## 4. Ejecución → Programación · hoja de ruta

Relación **Conformist**: Ejecución no reinterpreta ni negocia el contenido. Lo ejecuta tal cual.

```
GET /internal/v1/viajes/{viajeId}/hoja-de-ruta
```

Respuesta `200`:

```json
{
  "viajeId": "VIA-2026-00045",
  "estado": "DESPACHADO",
  "unidadId": "UNI-004",
  "conductorIds": ["CON-011"],
  "observaciones": "Coordinar con almacén del cliente antes de las 07:00.",
  "paradas": [
    { "secuencia": 1, "tipo": "CARGA",    "ordenDeServicioId": "ORD-2026-000123",
      "ubicacion": { "direccion": "Jr. Ayacucho 450", "distrito": "Cajamarca", "referencia": "Almacén 2", "contacto": "+51 976 000 111" },
      "horaEstimada": "2026-09-10T06:30:00-05:00" },
    { "secuencia": 2, "tipo": "DESCARGA", "ordenDeServicioId": "ORD-2026-000123",
      "ubicacion": { "direccion": "Av. España 1200", "distrito": "Trujillo", "referencia": "Puerta 3", "contacto": "+51 944 222 333" },
      "horaEstimada": "2026-09-10T14:00:00-05:00" }
  ]
}
```

| Estado | Cuándo |
|---|---|
| `200` | Viaje programado o despachado |
| `404` | El viaje no existe |
| `409` | El viaje está en `Planificado` o `Cancelado`: aún no hay hoja de ruta ejecutable |

El orden de `paradas` ya viene resuelto por VIA-06. Ejecución no lo recalcula.

---

## 5. Ejecución → Unidades · kilometraje y falla

Alimenta **UNI-03** y el programa de mantenimiento.

```
POST /internal/v1/unidades/{unidadId}/kilometraje
Idempotency-Key: <viajeId>:km-final
```

```json
{ "viajeId": "VIA-2026-00045", "kilometraje": 184320, "momento": "2026-09-10T21:15:00-05:00" }
```

| Estado | Cuándo |
|---|---|
| `200` | Registrado, o reintento con la misma clave |
| `409` | El kilometraje es menor al vigente (viola UNI-03) |

```
POST /internal/v1/unidades/{unidadId}/fallas
```

```json
{
  "viajeId": "VIA-2026-00045",
  "tipo": "MECANICA",
  "descripcion": "Pérdida de presión en circuito de frenos.",
  "momento": "2026-09-10T11:40:00-05:00",
  "dejaInoperativa": true
}
```

Con `dejaInoperativa: true`, Unidades marca `EstadoOperativo = INOPERATIVA` y la unidad deja de ser elegible
en el contrato 2 de forma inmediata.

---

## 6. Ejecución → Conductores · horas e incidencia

Alimenta **CON-02**.

```
POST /internal/v1/conductores/{conductorId}/horas-conduccion
Idempotency-Key: <viajeId>:<conductorId>:horas
```

```json
{ "viajeId": "VIA-2026-00045", "horas": 8.5, "desde": "2026-09-10T06:00:00-05:00", "hasta": "2026-09-10T14:30:00-05:00" }
```

| Estado | Cuándo |
|---|---|
| `200` | Registrado |
| `409` | Las horas acumuladas superarían el máximo normado (CON-02) |

```
POST /internal/v1/conductores/{conductorId}/incidencias
```

```json
{ "viajeId": "VIA-2026-00045", "tipo": "DOCUMENTARIA", "descripcion": "Retención SUTRAN por guía incompleta.", "atribuible": true }
```

---

## 7. Ejecución → Comercial · diferencia de carga y espera

Permite el reajuste de tarifa antes de facturar (ORD-01).

```
POST /internal/v1/ordenes/{ordenId}/diferencias-de-carga
```

```json
{
  "viajeId": "VIA-2026-00045",
  "declarado": { "pesoKg": 6000, "volumenM3": 18.0, "embalaje": "SACOS" },
  "real":      { "pesoKg": 8000, "volumenM3": 22.5, "embalaje": "SACOS" },
  "decision": "ACEPTADA_CON_REAJUSTE",
  "importeDelReajuste": { "monto": "320.00", "moneda": "PEN" },
  "momento": "2026-09-10T06:55:00-05:00"
}
```

`decision` ∈ `ACEPTADA_CON_REAJUSTE` · `ACEPTADA_PARCIAL` · `RECHAZADA`.

**Corrección sobre la versión anterior del contrato**, que no enviaba `importeDelReajuste`. Sin él,
Comercial no puede aplicar ORD-01: `OrdenDeServicio.reajustarCarga(...)` exige el importe para una orden
ya programada, y sin importe la operación no se puede completar. El número existe —Ejecución lo calcula
y lo manda en el contrato 8 como concepto `REAJUSTE`—, sólo que no viajaba aquí. Va como `null` cuando
la `decision` es `RECHAZADA`.

```
POST /internal/v1/ordenes/{ordenId}/esperas
```

```json
{ "viajeId": "VIA-2026-00045", "punto": "DESCARGA", "tiempoLibreHoras": 2.0, "tiempoRealHoras": 5.5, "excedenteHoras": 3.5 }
```

El `excedenteHoras` lo calcula Ejecución con `EsperaFacturable.excedente()`. Comercial lo consume, no lo recalcula.

---

## 8. Ejecución → Facturación · conformidad y conceptos

Dependencia crítica: sin esta llamada, Facturación queda bloqueada por **FAC-01**.

```
POST /internal/v1/conformidades
Idempotency-Key: <viajeId>:<ordenId>:conformidad
```

```json
{
  "viajeId": "VIA-2026-00045",
  "ordenDeServicioId": "ORD-2026-000123",
  "estado": "FIRMADA",
  "fechaDeFirma": "2026-09-10T15:20:00-05:00",
  "conceptosFacturables": [
    { "concepto": "ESTIBA",   "monto": "180.00", "moneda": "PEN" },
    { "concepto": "ESPERA",   "monto": "245.00", "moneda": "PEN", "detalle": "3.5 h sobre 2 h de tiempo libre" },
    { "concepto": "REAJUSTE", "monto": "320.00", "moneda": "PEN", "detalle": "2000 kg sobre lo declarado" }
  ],
  "incidenciasSinResolver": []
}
```

`estado` ∈ `FIRMADA` · `PARCIAL` · `RECHAZADA`.

`incidenciasSinResolver` no vacío hace que Facturación mantenga la factura en `BLOQUEADA` (FAC-05). El array
va siempre, aunque esté vacío: su ausencia es un error de contrato, no un «sin incidencias».

---

## 9. Facturación → Comercial · snapshot facturable

Relación **Conformist**: Facturación usa la tarifa y las condiciones exactamente como las definió Comercial.

```
GET /internal/v1/ordenes/{ordenId}/snapshot-facturable
```

Respuesta `200`:

```json
{
  "ordenId": "ORD-2026-000123",
  "clienteId": "CLI-0007",
  "ruc": "20481234567",
  "razonSocial": "Distribuidora Norte S.A.C.",
  "tarifa": {
    "fleteBase": { "monto": "1800.00", "moneda": "PEN" },
    "recargos":  [ { "tipo": "SOBRECAPACIDAD", "porcentaje": 10 } ],
    "descuento": { "porcentaje": 8, "motivo": "CONSOLIDACION" },
    "total":     { "monto": "1821.60", "moneda": "PEN" }
  },
  "condicionDePago": { "modalidad": "CREDITO", "plazoEnDias": 30 },
  "tomadoEn": "2026-09-10T16:00:00-05:00"
}
```

Es un **snapshot inmutable**: Facturación lo persiste tal cual y no vuelve a consultarlo. Un cambio posterior
en Comercial no altera una factura ya emitida (FAC-03); se corrige con nota de crédito.

`tarifa.total` lo calcula Comercial con `Tarifa.total()`. Facturación no reaplica recargos ni descuentos.

---

## 10. Facturación → Cobranza · cuenta por cobrar

Relación **Conformist**: el formato lo impone SUNAT; Cobranza lo consume tal cual.

```
POST /internal/v1/cuentas-por-cobrar
Idempotency-Key: <facturaId>
```

```json
{
  "facturaId": "FAC-2026-000310",
  "documentoId": "F001-00000310",
  "clienteId": "CLI-0007",
  "total":      { "monto": "1821.60", "moneda": "PEN" },
  "detraccion": { "porcentaje": 4, "monto": "72.86", "moneda": "PEN", "cuentaBancaria": "00-123-456789" },
  "montoNeto":  { "monto": "1748.74", "moneda": "PEN" },
  "fechaDeEmision": "2026-09-10T16:30:00-05:00",
  "fechaDeVencimiento": "2026-10-10T23:59:59-05:00",
  "condicionDePago": { "modalidad": "CREDITO", "plazoEnDias": 30 }
}
```

| Estado | Cuándo |
|---|---|
| `201` | Cuenta creada |
| `200` | Reintento con la misma `Idempotency-Key` |
| `422` | `montoNeto + detraccion.monto ≠ total` (viola FAC-04) |

Sólo las facturas a crédito entran a la cartera. Las facturas al contado se cobran contra entrega y se
registran ya canceladas.

---

## 11. Comercial → Cobranza · estado crediticio

Sostiene **CLI-01** y **ORD-02**. Comercial consulta **antes** de aceptar una orden a crédito.

```
GET /internal/v1/clientes/{clienteId}/estado-crediticio
```

Respuesta `200`:

```json
{
  "clienteId": "CLI-0007",
  "situacion": "SUSPENDIDO",
  "fechaDeCambio": "2026-08-28",
  "diasDeAtrasoMaximo": 43,
  "cuentasVencidas": 2,
  "deudaPorMoneda": [ { "monto": "5420.30", "moneda": "PEN" }, { "monto": "800.00", "moneda": "USD" } ]
}
```

`situacion` ∈ `VIGENTE` · `SUSPENDIDO`.

**Corrección sobre la versión anterior del contrato**, que traía un único `deudaTotal`. Un cliente puede
deber flete local en soles y flete de exportación en dólares a la vez, y un único total obligaría a
convertir a un tipo de cambio que Cobranza no conoce. `CuentaCorrienteDelCliente.deudaTotal(moneda)`
exige la moneda justamente para que nadie la adivine, así que el contrato lleva un importe por cada
moneda con deuda viva, y la lista va vacía cuando el cliente no debe nada.

Lo que sostiene CLI-01 y ORD-02 es `situacion`; la deuda es informativa.

**Segunda corrección: `fechaDeCambio` es una fecha, no una marca de tiempo.** El contrato la escribía
como `2026-08-28T09:00:00-05:00` y los dos contextos la modelan como `LocalDate`, así que el proveedor
llevaba desde `S4` publicando `2026-08-28` y nadie lo había mirado: el ejemplo del documento no estaba
comprobado contra nada. Manda el código, y no por costumbre — una suspensión de crédito se decide un
día, no a una hora, y `CuentaCorrienteDelCliente` compara por día. La regla 6 pide offset para los
instantes; una fecha sin hora es ISO 8601 igual. Lo destapó `CobranzaClientStubTest`, que decodifica el
ejemplo de este documento con el cliente real: es exactamente para lo que existe.

**Comportamiento ante indisponibilidad — decisión explícita:** si Cobranza no responde, Comercial **rechaza**
la orden a crédito con `503` y un `problem+json` que indica que el estado crediticio no pudo verificarse. No
se asume `VIGENTE`. La orden al contado sí procede, porque no depende de este contrato.

---

## Estado de implementación

**Los once contratos tienen su lado proveedor implementado y probado** (slice `S4-api-interna`). Falta el
lado consumidor: los clientes Feign, con su timeout y su traducción de fallo remoto, que son de `S5`.
Hasta entonces ningún contrato está terminado según el criterio de `docs/api/README.md`, que exige la
prueba del cliente contra un stub.

| # | Proveedor | Endpoint | Estado |
|---|---|---|---|
| 1 | Comercial | `GET /internal/v1/ordenes/{ordenId}` | proveedor **done** · cliente pendiente |
| 2 | Unidades | `GET /internal/v1/unidades/{id}/elegibilidad` | proveedor **done** · cliente pendiente |
| 3 | Conductores | `GET /internal/v1/conductores/{id}/elegibilidad` | proveedor **done** · cliente pendiente |
| 4 | Programación | `GET /internal/v1/viajes/{id}/hoja-de-ruta` | proveedor **done** · cliente pendiente |
| 5 | Unidades | `POST .../kilometraje` y `.../fallas` | proveedor **done** · cliente pendiente |
| 6 | Conductores | `POST .../horas-conduccion` y `.../incidencias` | proveedor **done** · cliente pendiente |
| 7 | Comercial | `POST .../diferencias-de-carga` y `.../esperas` | proveedor **done** · cliente pendiente |
| 8 | Facturación | `POST /internal/v1/conformidades` | proveedor **done** · cliente pendiente |
| 9 | Comercial | `GET /internal/v1/ordenes/{id}/snapshot-facturable` | proveedor **done** · cliente pendiente |
| 10 | Cobranza | `POST /internal/v1/cuentas-por-cobrar` | proveedor **done** · cliente pendiente |
| 11 | Cobranza | `GET /internal/v1/clientes/{id}/estado-crediticio` | proveedor **done** · cliente pendiente |

### Lo que `S4` cambió de estos contratos

Escribir el lado proveedor con este documento delante destapó que el dominio se había quedado corto en
tres sitios, y que dos códigos HTTP estaban mal:

| Qué | Dónde |
|---|---|
| La parada guardaba la ubicación como un texto suelto; el contrato 4 pide cuatro campos | `Ubicacion` entra como objeto de valor en Programación |
| La orden no tenía embalaje, naturaleza, distancia, ventana ni tipo de unidad | Cinco campos nuevos en Comercial, y `EsperaRegistrada` para el contrato 7 |
| Conductores no tenía dónde poner una incidencia de ruta | `Incidencia` entra como entidad hija |
| CON-02 caía en el `422` por defecto; el contrato 6 dice `409` | Las horas se reponen mañana: es «ahora no» |
| UNI-03 caía en el `422` por defecto; el contrato 5 dice `409` | Un reporte que llega tarde trae una lectura que ya quedó atrás |

Ninguna de las dos excepciones se alcanzaba desde `S3`, así que su código nunca se había puesto a prueba
contra el contrato.
