-- Contexto: Ejecucion y Seguimiento. Esquema mirador_ejecucion.
--
-- LIQ-02 se lee tambien aqui: la tabla liquidaciones NO tiene columna de saldo. El saldo se
-- calcula desde el anticipo y los gastos. Una columna saldo seria el defecto que la invariante
-- prohibe, y la prueba de dominio lo comprueba por reflexion.

CREATE TABLE checklists (
    id                VARCHAR(40) NOT NULL,
    checklist_aprobado BIT(1)     NOT NULL,
    checklist_momento DATETIME(6) NOT NULL,
    CONSTRAINT pk_checklists PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE checklist_observaciones (
    checklist_id VARCHAR(40)  NOT NULL,
    observacion  VARCHAR(300) NOT NULL,
    CONSTRAINT fk_observaciones_checklist FOREIGN KEY (checklist_id) REFERENCES checklists (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE ejecuciones (
    viaje_id            VARCHAR(40) NOT NULL,
    unidad_ejecutora_id VARCHAR(40) NOT NULL,
    estado              VARCHAR(20) NOT NULL,
    checklist_id        VARCHAR(40) NULL,
    fecha_inicio        DATETIME(6) NULL,
    fecha_entrega       DATETIME(6) NULL,
    CONSTRAINT pk_ejecuciones PRIMARY KEY (viaje_id),
    CONSTRAINT fk_ejecuciones_checklist FOREIGN KEY (checklist_id) REFERENCES checklists (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- EJV-05: el transbordo cambia la unidad ejecutora y apila la anterior, conservando el viaje.
CREATE TABLE ejecucion_unidades_anteriores (
    ejecucion_id VARCHAR(40) NOT NULL,
    unidad_id    VARCHAR(40) NOT NULL,
    CONSTRAINT fk_unidades_anteriores_ejecucion FOREIGN KEY (ejecucion_id) REFERENCES ejecuciones (viaje_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE conformidades (
    id                   VARCHAR(40)  NOT NULL,
    orden_de_servicio_id VARCHAR(40)  NOT NULL,
    estado               VARCHAR(20)  NOT NULL,
    recibido_por         VARCHAR(200) NULL,
    fecha_de_firma       DATETIME(6)  NULL,
    observaciones        VARCHAR(500) NULL,
    CONSTRAINT pk_conformidades PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE paradas (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    ejecucion_id          VARCHAR(40)  NOT NULL,
    secuencia             INT          NOT NULL,
    orden_de_servicio_id  VARCHAR(40)  NOT NULL,
    direccion             VARCHAR(300) NULL,
    estado                VARCHAR(20)  NOT NULL,
    conformidad_id        VARCHAR(40)  NULL,
    espera_inicio         DATETIME(6)  NULL,
    espera_fin            DATETIME(6)  NULL,
    espera_tiempo_libre   INT          NULL,
    CONSTRAINT pk_paradas PRIMARY KEY (id),
    CONSTRAINT fk_paradas_ejecucion FOREIGN KEY (ejecucion_id) REFERENCES ejecuciones (viaje_id),
    CONSTRAINT fk_paradas_conformidad FOREIGN KEY (conformidad_id) REFERENCES conformidades (id),
    -- EJV-02: una conformidad por parada, y las paradas no repiten secuencia dentro del viaje.
    CONSTRAINT uq_paradas_ejecucion_secuencia UNIQUE (ejecucion_id, secuencia)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE hitos (
    id           VARCHAR(40)  NOT NULL,
    ejecucion_id VARCHAR(40)  NOT NULL,
    tipo         VARCHAR(30)  NOT NULL,
    momento      DATETIME(6)  NOT NULL,
    ubicacion    VARCHAR(300) NULL,
    CONSTRAINT pk_hitos PRIMARY KEY (id),
    CONSTRAINT fk_hitos_ejecucion FOREIGN KEY (ejecucion_id) REFERENCES ejecuciones (viaje_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE incidencias (
    id                    VARCHAR(40)  NOT NULL,
    ejecucion_id          VARCHAR(40)  NOT NULL,
    tipo                  VARCHAR(30)  NOT NULL,
    descripcion           VARCHAR(500) NOT NULL,
    resuelta              BIT(1)       NOT NULL,
    momento               DATETIME(6)  NOT NULL,
    evidencia_descripcion VARCHAR(500) NULL,
    evidencia_momento     DATETIME(6)  NULL,
    CONSTRAINT pk_incidencias PRIMARY KEY (id),
    CONSTRAINT fk_incidencias_ejecucion FOREIGN KEY (ejecucion_id) REFERENCES ejecuciones (viaje_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- La evidencia es obligatoria en DANIO, FALTANTE y RECHAZO_DE_CARGA. Esa regla la sostiene el
-- agregado, no la tabla: aqui las columnas son nulas porque otros tipos no la exigen.
CREATE TABLE incidencia_fotografias (
    incidencia_id VARCHAR(40)  NOT NULL,
    fotografia    VARCHAR(500) NOT NULL,
    CONSTRAINT fk_fotografias_incidencia FOREIGN KEY (incidencia_id) REFERENCES incidencias (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE liquidaciones (
    viaje_id            VARCHAR(40)   NOT NULL,
    conductor_id        VARCHAR(40)   NOT NULL,
    anticipo_monto      DECIMAL(15,2) NOT NULL,
    anticipo_moneda     VARCHAR(3)    NOT NULL,
    estado              VARCHAR(20)   NOT NULL,
    fecha_de_aprobacion DATETIME(6)   NULL,
    motivo_observacion  VARCHAR(300)  NULL,
    -- En un viaje con relevo hay dos liquidaciones sobre el mismo viaje, una por conductor.
    CONSTRAINT pk_liquidaciones PRIMARY KEY (viaje_id, conductor_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE gastos_de_ruta (
    id                       VARCHAR(40)   NOT NULL,
    liquidacion_viaje_id     VARCHAR(40)   NOT NULL,
    liquidacion_conductor_id VARCHAR(40)   NOT NULL,
    concepto                 VARCHAR(20)   NOT NULL,
    importe_monto            DECIMAL(15,2) NOT NULL,
    importe_moneda           VARCHAR(3)    NOT NULL,
    comprobante_tipo         VARCHAR(30)   NOT NULL,
    comprobante_numero       VARCHAR(40)   NOT NULL,
    comprobante_fecha        DATETIME(6)   NOT NULL,
    descripcion              VARCHAR(300)  NULL,
    CONSTRAINT pk_gastos_de_ruta PRIMARY KEY (id),
    CONSTRAINT fk_gastos_liquidacion FOREIGN KEY (liquidacion_viaje_id, liquidacion_conductor_id)
        REFERENCES liquidaciones (viaje_id, conductor_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX ix_ejecuciones_estado ON ejecuciones (estado);
CREATE INDEX ix_ejecuciones_unidad ON ejecuciones (unidad_ejecutora_id);
CREATE INDEX ix_liquidaciones_estado ON liquidaciones (estado);
