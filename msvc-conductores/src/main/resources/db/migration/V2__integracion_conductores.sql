-- Slice S4-api-interna. Lo que los contratos 3 y 6 necesitan y el dominio no tenia.
--
-- El esquema lo versiona Flyway, nunca Hibernate: con ddl-auto=validate una entidad sin su
-- migracion rompe el build, y es deliberado.

-- Contrato 6: Ejecucion reporta las incidencias del viaje al terminar. Entidad hija del agregado
-- Conductor, igual que la induccion: se guarda y se borra con el, y no tiene repositorio propio.
-- viaje_id es un identificador escalar sin FK; el viaje vive en msvc-programacion.
CREATE TABLE incidencias (
    id           VARCHAR(40)  NOT NULL,
    conductor_id VARCHAR(40)  NOT NULL,
    viaje_id     VARCHAR(40)  NOT NULL,
    tipo         VARCHAR(40)  NOT NULL,
    descripcion  VARCHAR(500) NOT NULL,
    atribuible   BIT(1)       NOT NULL,
    registrada_en  DATETIME(6)  NOT NULL,
    CONSTRAINT pk_incidencias PRIMARY KEY (id),
    CONSTRAINT fk_incidencias_conductor FOREIGN KEY (conductor_id) REFERENCES conductores (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Regla 6 de los contratos: un reintento con la misma Idempotency-Key devuelve el resultado
-- original y no repite el efecto. La clave la construye el consumidor; aqui se guarda tal cual.
-- La escritura del efecto y el registro de la clave van en la misma transaccion: separarlos deja
-- una ventana en la que el efecto ya se aplico y el reintento lo duplicaria.
CREATE TABLE peticiones_idempotentes (
    clave         VARCHAR(200) NOT NULL,
    recurso_id    VARCHAR(40)  NOT NULL,
    registrada_en DATETIME(6)  NOT NULL,
    CONSTRAINT pk_peticiones_idempotentes PRIMARY KEY (clave)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX ix_incidencias_conductor ON incidencias (conductor_id);
CREATE INDEX ix_incidencias_viaje ON incidencias (viaje_id);
