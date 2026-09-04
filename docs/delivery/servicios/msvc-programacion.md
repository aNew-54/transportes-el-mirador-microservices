# msvc-programacion — Programación y Despacho

| | |
|---|---|
| Bounded context | Programación y Despacho |
| Subdominio | **Core Domain** |
| Puerto | `8020` |
| Esquema | `mirador_programacion` |
| Paquete raíz | `pe.edu.unc.elmirador.programacion` |
| Responsable de revisión | Brayam Alfaro |
| Agregados | 3 |
| Invariantes | 11 (VIA-01…07, AGU-01/02, AGC-01/02) |

## Responsabilidad

Es el Core Domain. Decide qué órdenes se consolidan en un mismo viaje, qué unidad y qué conductor lo
ejecutan, en qué orden se recorren las paradas, y emite la hoja de ruta.

Es la actividad que más valor agrega y la que más diferencia a la empresa: optimiza el uso de la flota y
reduce el retorno vacío.

> **Este servicio concentra el «20 % duro».** La consolidación (VIA-02…VIA-05) y la secuencia de estiba
> (VIA-06) las implementa Claude, no el ejecutor. El resto del módulo sí se delega.

## Agregados

### `Viaje` — raíz `Viaje`

- **Objetos de valor**: `Ruta`, `VentanaDeTiempo`, `CargaConsolidada`, `AsignaciónDeRecursos`,
  `EstadoDeViaje`, `HojaDeRuta` (que contiene las `Parada` planificadas)
- **Referencias**: `OrdenDeServicioId` (varias), `UnidadId`, `ConductorId`
- **Métodos**: `consolidarOrden(orden)`, `asignarRecursos(unidad, conductores)`, `confirmarProgramacion()`,
  `autorizarDespacho()`
- **Invariantes**: VIA-01 … VIA-07

`EstadoDeViaje` ∈ `PLANIFICADO` · `PROGRAMADO` · `DESPACHADO` · `CANCELADO`, y valida las transiciones
permitidas. No se salta estados.

Métodos de los objetos de valor, con el nombre exacto del diseño táctico:

| Objeto de valor | Método | Qué resuelve |
|---|---|---|
| `CargaConsolidada` | `pesoTotal()`, `cabeEn(capacidad)` | VIA-02 |
| `Ruta` | `mismoCorredorQue(otra)` | VIA-03 |
| `VentanaDeTiempo` | `seSolapaCon(otra)` | VIA-03, AGU-01, AGC-01 |
| `Carga` | `esCompatibleCon(otra)` | VIA-05 |
| `HojaDeRuta` | `secuenciaDeEstiba()` | VIA-06 |
| `AsignaciónDeRecursos` | `esCompleta()` | VIA-01 |

`AsignaciónDeRecursos` admite un segundo conductor **únicamente** en viajes con relevo (caso Lima, donde el
trayecto supera las horas de conducción continua permitidas).

`Parada` es inmutable: si el itinerario cambia, no se modifica la parada — se emite una hoja de ruta nueva.

### `AgendaDeUnidad` — raíz `AgendaDeUnidad`, entidad hija `ReservaDeUnidad`

- **Identidad**: la unidad misma (`UnidadId`)
- **Objetos de valor**: `VentanaDeTiempo`, `EstadoDeReserva`
- **Métodos**: `reservar(ventana)`, `confirmar(reserva)`, `liberar(reserva)`
- **Invariantes**: AGU-01, AGU-02

`EstadoDeReserva` ∈ `TENTATIVA` · `CONFIRMADA` · `LIBERADA`. Sólo las dos primeras bloquean el recurso.

### `AgendaDeConductor` — raíz `AgendaDeConductor`, entidad hija `ReservaDeConductor`

- **Identidad**: el conductor mismo (`ConductorId`)
- Mismos objetos de valor, métodos e invariantes que la agenda de unidad, aplicados al recurso humano
- **Invariantes**: AGC-01, AGC-02

## API pública `/api/v1`

| Método | Ruta | Qué hace | Códigos |
|---|---|---|---|
| `POST` | `/viajes` | Crea un viaje en `PLANIFICADO` a partir de una orden confirmada | `201` `404` `409` |
| `POST` | `/viajes/{id}/ordenes` | Consolida otra orden en el viaje | `200` `409` (VIA-02…05, VIA-07) |
| `POST` | `/viajes/{id}/recursos` | Asigna unidad y conductores verificando elegibilidad | `200` `409` `503` |
| `POST` | `/viajes/{id}/programar` | Pasa a `PROGRAMADO` y emite la hoja de ruta | `200` `409` (VIA-01) |
| `POST` | `/viajes/{id}/despachar` | Pasa a `DESPACHADO` | `200` `409` |
| `POST` | `/viajes/{id}/cancelar` | Cancela y libera las reservas | `200` `409` |
| `GET` | `/viajes/{id}` | Consulta el viaje | `200` `404` |
| `GET` | `/agendas/unidades/{unidadId}` | Consulta la agenda de una unidad | `200` `404` |
| `GET` | `/agendas/conductores/{conductorId}` | Consulta la agenda de un conductor | `200` `404` |

## API interna `/internal/v1`

Publica el contrato **4**.

| Método | Ruta | Consumidor | Contrato |
|---|---|---|---|
| `GET` | `/viajes/{viajeId}/hoja-de-ruta` | Ejecución | 4 |

## Clientes Feign que consume

| Cliente | Servicio | Contrato | Propiedad |
|---|---|---|---|
| `ComercialClient` | Comercial | 1 | `clients.comercial.url` |
| `UnidadesClient` | Unidades | 2 | `clients.unidades.url` |
| `ConductoresClient` | Conductores | 3 | `clients.conductores.url` |

`POST /viajes/{id}/recursos` no asigna nada si Unidades o Conductores no responden: devuelve `503`. Una
elegibilidad no verificable **no** se trata como elegible.

## Criterios de éxito

- [ ] `./mvnw -pl msvc-programacion verify` en verde (exige Docker: levanta MySQL con Testcontainers)
- [ ] Cada tabla del contexto creada por una migración Flyway; `ddl-auto=validate` en verde
- [ ] `PersistenciaProgramacionIT` en verde contra MySQL real
- [ ] Las 11 invariantes con prueba que las viola
- [ ] `CargaConsolidada.cabeEn()` probado en el límite exacto de capacidad, no sólo por encima y por debajo
- [ ] `VentanaDeTiempo.seSolapaCon()` probado con solape parcial, contención total y bordes que se tocan
- [ ] `HojaDeRuta.secuenciaDeEstiba()` devuelve el orden inverso de descarga, con prueba de tres paradas
- [ ] Transiciones de `EstadoDeViaje` probadas, incluidas las prohibidas
- [ ] Los 3 clientes Feign con timeout, traducción de error y prueba con stub que cubre el `503`
- [ ] `GET /internal/v1/viajes/{id}/hoja-de-ruta` devuelve `409` para un viaje en `PLANIFICADO`
- [ ] 0 imports de otro contexto
- [ ] Sano en `./scripts/smoke-test.sh`
