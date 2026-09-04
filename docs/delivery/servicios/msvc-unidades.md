# msvc-unidades — Gestión de Unidades

| | |
|---|---|
| Bounded context | Gestión de Unidades |
| Subdominio | Support |
| Puerto | `8040` |
| Esquema | `mirador_unidades` |
| Paquete raíz | `pe.edu.unc.elmirador.unidades` |
| Responsable de revisión | Arnold Ocas |
| Agregados | 3 |
| Invariantes | 6 (UNI-01/02/03, OMT-01/02, REP-01) |

## Responsabilidad

Mantener la hoja de vida de las catorce unidades de la flota, programar el mantenimiento preventivo por
kilometraje, controlar la vigencia de los cuatro documentos obligatorios y administrar el stock de repuestos
críticos.

Su salida hacia el Core es una sola: **decir si una unidad puede o no ser asignada a un viaje**.

Está en relación `Partnership` con `msvc-conductores`: ambos los administra el encargado de flota y
evolucionan de forma coordinada.

## Agregados

### `Unidad` — raíz `Unidad`, entidad hija `DocumentoVehicular`

- **Objetos de valor**: `Placa`, `TipoDeUnidad`, `Capacidad`, `Kilometraje`, `EstadoOperativo`,
  `ProgramaDeMantenimiento`, `TipoDeDocumento`, `PeriodoDeVigencia`
- **Métodos**: `actualizarKilometraje(km)`, `estáHabilitada()`, `marcarInoperativa(motivo)`
- **Invariantes**: UNI-01, UNI-02, UNI-03

`TipoDeUnidad` ∈ `FURGON` · `PLATAFORMA` · `CAMA_BAJA`, con `admite(carga)` y `licenciaRequerida()`.
La maquinaria pesada exige cama baja; la carga paletizada va en furgón o plataforma.

`EstadoOperativo` ∈ `OPERATIVA` · `EN_TALLER` · `INOPERATIVA`, con motivo. Sólo `OPERATIVA` es asignable.

`DocumentoVehicular` cubre los cuatro documentos obligatorios: revisión técnica, SOAT, permiso de operación
del MTC y certificado de habilitación vehicular. Con `estáVigente()` en falso para **cualquiera** de ellos,
la unidad pasa automáticamente a `INOPERATIVA` (UNI-01). No es una alerta: es un cambio de estado.

`ProgramaDeMantenimiento` guarda `kmÚltimoServicio`, `kmPróximoServicio` e `intervalo`, con
`estáVencido(km)` y `requiereAlerta(km)`. La alerta se dispara a 500 km del próximo servicio.
Intervalos del negocio: 10 000 km (aceite y filtros), 20 000 km (revisión mayor), 40 000 km (llantas).

`Kilometraje` impide registrar un valor menor al vigente (UNI-03).

### `OrdenDeMantenimiento` — raíz `OrdenDeMantenimiento`, entidad hija `TrabajoRealizado`

- **Objetos de valor**: `TipoDeMantenimiento`, `Kilometraje`, `Dinero`
- **Referencias**: `UnidadId`, `RepuestoId`
- **Método**: `cerrar()`
- **Invariantes**: OMT-01, OMT-02

`TipoDeMantenimiento` ∈ `PREVENTIVO` · `CORRECTIVO`.

### `Repuesto` — raíz `Repuesto`

- **Objetos de valor**: `Dinero`
- **Métodos**: `ajustarInventario(cantidad)`, `requiereReposicion()`
- **Invariante**: REP-01

## API pública `/api/v1`

| Método | Ruta | Qué hace | Códigos |
|---|---|---|---|
| `POST` | `/unidades` | Registra una unidad | `201` `400` `409` |
| `GET` | `/unidades/{id}` | Consulta la hoja de vida | `200` `404` |
| `GET` | `/unidades` | Lista con filtro por estado operativo | `200` |
| `POST` | `/unidades/{id}/documentos` | Registra o renueva un documento | `201` `400` |
| `POST` | `/unidades/{id}/estado` | Cambia el estado operativo con motivo | `200` `409` |
| `POST` | `/ordenes-mantenimiento` | Abre una orden de taller | `201` `422` (OMT-02) |
| `POST` | `/ordenes-mantenimiento/{id}/trabajos` | Registra un trabajo | `201` `409` (OMT-01) |
| `POST` | `/ordenes-mantenimiento/{id}/cerrar` | Cierra la orden y actualiza el programa | `200` `409` |
| `POST` | `/repuestos` | Registra un repuesto con su stock mínimo | `201` `400` |
| `POST` | `/repuestos/{id}/movimientos` | Ajusta el inventario | `200` `422` (REP-01) |
| `GET` | `/alertas` | Documentos por vencer y mantenimientos próximos | `200` |

## API interna `/internal/v1`

Publica los contratos **2** y **5**.

| Método | Ruta | Consumidor | Contrato |
|---|---|---|---|
| `GET` | `/unidades/{unidadId}/elegibilidad` | Programación | 2 |
| `POST` | `/unidades/{unidadId}/kilometraje` | Ejecución | 5 |
| `POST` | `/unidades/{unidadId}/fallas` | Ejecución | 5 |

El endpoint de elegibilidad concentra UNI-01, UNI-02, la capacidad y la compatibilidad de tipo. Devuelve
`200` con `elegible: false` y la lista de motivos; **no** devuelve error.

## Clientes Feign que consume

Ninguno. Unidades es un proveedor puro.

## Criterios de éxito

- [ ] `./mvnw -pl msvc-unidades verify` en verde (exige Docker: levanta MySQL con Testcontainers)
- [ ] Cada tabla del contexto creada por una migración Flyway; `ddl-auto=validate` en verde
- [ ] `PersistenciaUnidadesIT` en verde contra MySQL real
- [ ] Las 6 invariantes con prueba que las viola
- [ ] Prueba de UNI-01 para los cuatro tipos de documento, uno por uno
- [ ] Prueba de que `actualizarKilometraje()` rechaza un retroceso (UNI-03)
- [ ] `ProgramaDeMantenimiento.requiereAlerta()` probado en el borde de los 500 km
- [ ] `GET /internal/v1/unidades/{id}/elegibilidad` devuelve `200` con `elegible:false` y motivo, no un `4xx`
- [ ] `POST .../kilometraje` devuelve `409` ante un retroceso, e idempotencia probada
- [ ] `POST .../fallas` con `dejaInoperativa:true` deja la unidad no elegible en la consulta siguiente
- [ ] 0 imports de otro contexto
- [ ] Sano en `./scripts/smoke-test.sh`
