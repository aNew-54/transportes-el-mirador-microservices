# msvc-conductores — Gestión de Conductores

| | |
|---|---|
| Bounded context | Gestión de Conductores |
| Subdominio | Support |
| Puerto | `8050` |
| Esquema | `mirador_conductores` |
| Paquete raíz | `pe.edu.unc.elmirador.conductores` |
| Responsable de revisión | Brayam Alfaro |
| Agregados | 1 |
| Invariantes | 3 (CON-01, CON-02, CON-03) |

## Responsabilidad

Controlar la vigencia y categoría de la licencia de cada uno de los dieciocho conductores, la vigencia de las
inducciones de seguridad que exigen los clientes mineros, el acumulado de horas de conducción y descansos, y
el historial de viajes e incidencias atribuibles.

Es el servicio más pequeño en superficie y uno de los más críticos en efecto: su única salida hacia el Core
decide si un conductor puede ser asignado.

Está en relación `Partnership` con `msvc-unidades`.

## Agregados

### `Conductor` — raíz `Conductor`, entidad hija `Inducción`

- **Objetos de valor**: `NúmeroDeLicencia`, `CategoríaDeLicencia`, `PeriodoDeVigencia`,
  `HorasDeConducción`, `EstadoDeHabilitación`
- **Referencias**: `ClienteId`, en la inducción exigida
- **Métodos**: `estáHabilitadoPara(tipoUnidad)`, `acumularHoras(horas)`
- **Invariantes**: CON-01, CON-02, CON-03

`CategoríaDeLicencia.habilitaPara(tipoDeUnidad)` decide qué camión puede conducir un chofer con esa
categoría. Es la contraparte de `TipoDeUnidad.licenciaRequerida()` en `msvc-unidades`; ambas deben
coincidir, y esa coincidencia se verifica en el contrato 3, no por acoplamiento entre módulos.

`HorasDeConducción` guarda `horas` y `ventanaDeCómputo`, con `tieneDisponibles(requeridas)`. El acumulado se
mide dentro del periodo normado por la regulación de descansos, no de forma indefinida.

`EstadoDeHabilitación` lleva `situacion` y `motivo`. Los motivos son los del contrato 3:
`LICENCIA_VENCIDA`, `CATEGORIA_INSUFICIENTE`, `HORAS_INSUFICIENTES`, `INDUCCION_VENCIDA:<clienteId>`.

`Inducción` referencia el `ClienteId` que la exige y tiene su propio `PeriodoDeVigencia`; se renueva
anualmente. Un conductor puede estar habilitado en general y no habilitado para un cliente concreto (CON-03):
la habilitación **no es un booleano global**.

## API pública `/api/v1`

| Método | Ruta | Qué hace | Códigos |
|---|---|---|---|
| `POST` | `/conductores` | Registra un conductor con su licencia | `201` `400` `409` |
| `GET` | `/conductores/{id}` | Consulta el legajo | `200` `404` |
| `GET` | `/conductores` | Lista con filtro por estado de habilitación | `200` |
| `POST` | `/conductores/{id}/licencia` | Renueva la licencia o cambia la categoría | `200` `400` |
| `POST` | `/conductores/{id}/inducciones` | Registra una inducción para un cliente | `201` `400` |
| `GET` | `/conductores/{id}/horas` | Consulta el acumulado en la ventana vigente | `200` `404` |
| `POST` | `/conductores/{id}/descanso` | Registra un descanso y libera horas | `200` `409` |
| `GET` | `/alertas` | Licencias e inducciones por vencer | `200` |

## API interna `/internal/v1`

Publica los contratos **3** y **6**.

| Método | Ruta | Consumidor | Contrato |
|---|---|---|---|
| `GET` | `/conductores/{conductorId}/elegibilidad` | Programación | 3 |
| `POST` | `/conductores/{conductorId}/horas-conduccion` | Ejecución | 6 |
| `POST` | `/conductores/{conductorId}/incidencias` | Ejecución | 6 |

El endpoint de elegibilidad concentra las tres invariantes. `clienteId` es opcional: sólo llega cuando el
destino exige inducción. Sin ese parámetro, CON-03 no se evalúa.

## Clientes Feign que consume

Ninguno. Conductores es un proveedor puro.

## Criterios de éxito

- [ ] `./mvnw -pl msvc-conductores verify` en verde (exige Docker: levanta MySQL con Testcontainers)
- [ ] Cada tabla del contexto creada por una migración Flyway; `ddl-auto=validate` en verde
- [ ] `PersistenciaConductoresIT` en verde contra MySQL real
- [ ] Las 3 invariantes con prueba que las viola
- [ ] `CategoríaDeLicencia.habilitaPara()` probado para las tres categorías contra los tres tipos de unidad
- [ ] Prueba de CON-03: mismo conductor elegible sin `clienteId` y no elegible con un `clienteId` cuya
      inducción está vencida
- [ ] `HorasDeConducción.tieneDisponibles()` probado en el borde exacto del máximo normado
- [ ] `POST .../horas-conduccion` devuelve `409` cuando el acumulado superaría el máximo (CON-02)
- [ ] Idempotencia probada en los dos `POST` del contrato 6
- [ ] 0 imports de otro contexto
- [ ] Sano en `./scripts/smoke-test.sh`
