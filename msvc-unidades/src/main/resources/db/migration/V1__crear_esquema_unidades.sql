-- Contexto: Gestion de Unidades. Esquema mirador_unidades.
--
-- El esquema lo versiona Flyway, nunca Hibernate: con ddl-auto=validate una entidad sin su
-- migracion rompe el build, y es deliberado.
--
-- Las claves foraneas viven SOLO dentro del contexto. Las referencias dentro del contexto
-- (unidad_id en ordenes_mantenimiento y repuesto_id en trabajos_realizados) llevan FK.

CREATE TABLE unidades (
    id                      VARCHAR(40)   NOT NULL,
    placa                   VARCHAR(10)   NOT NULL,
    tipo                    VARCHAR(20)   NOT NULL,
    peso_maximo_kg          INT           NOT NULL,
    volumen_maximo_m3       DECIMAL(10,2) NOT NULL,
    kilometraje             INT           NOT NULL,
    situacion_operativa     VARCHAR(20)   NOT NULL,
    motivo_estado           VARCHAR(300)  NULL,
    km_ultimo_servicio      INT           NOT NULL,
    km_proximo_servicio     INT           NOT NULL,
    intervalo_mantenimiento VARCHAR(30)   NOT NULL,
    CONSTRAINT pk_unidades PRIMARY KEY (id),
    CONSTRAINT uq_unidades_placa UNIQUE (placa)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE documentos_vehiculares (
    id              VARCHAR(40)  NOT NULL,
    unidad_id       VARCHAR(40)  NOT NULL,
    tipo_documento  VARCHAR(30)  NOT NULL,
    vigente_desde   DATE         NOT NULL,
    vigente_hasta   DATE         NOT NULL,
    numero          VARCHAR(50)  NULL,
    CONSTRAINT pk_documentos_vehiculares PRIMARY KEY (id),
    CONSTRAINT fk_documentos_vehiculares_unidad FOREIGN KEY (unidad_id) REFERENCES unidades (id),
    CONSTRAINT uq_documentos_vehiculares_unidad_tipo UNIQUE (unidad_id, tipo_documento)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE repuestos (
    id                    VARCHAR(40)   NOT NULL,
    codigo                VARCHAR(50)   NOT NULL,
    descripcion           VARCHAR(300)  NOT NULL,
    existencias           INT           NOT NULL,
    stock_minimo          INT           NOT NULL,
    costo_unitario_monto  DECIMAL(10,2) NULL,
    costo_unitario_moneda VARCHAR(3)    NULL,
    CONSTRAINT pk_repuestos PRIMARY KEY (id),
    CONSTRAINT uq_repuestos_codigo UNIQUE (codigo)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE ordenes_mantenimiento (
    id                 VARCHAR(40) NOT NULL,
    unidad_id          VARCHAR(40) NOT NULL,
    tipo_mantenimiento VARCHAR(20) NOT NULL,
    km_atencion        INT         NOT NULL,
    estado             VARCHAR(20) NOT NULL,
    fecha_apertura     DATE        NOT NULL,
    fecha_cierre       DATE        NULL,
    codigo_moneda      VARCHAR(3)  NOT NULL,
    CONSTRAINT pk_ordenes_mantenimiento PRIMARY KEY (id),
    CONSTRAINT fk_ordenes_mantenimiento_unidad FOREIGN KEY (unidad_id) REFERENCES unidades (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE trabajos_realizados (
    id                     VARCHAR(40)   NOT NULL,
    orden_id               VARCHAR(40)   NOT NULL,
    descripcion            VARCHAR(300)  NOT NULL,
    costo_mano_obra_monto  DECIMAL(10,2) NOT NULL,
    costo_mano_obra_moneda VARCHAR(3)    NOT NULL,
    repuesto_id            VARCHAR(40)   NULL,
    cantidad               INT           NOT NULL,
    CONSTRAINT pk_trabajos_realizados PRIMARY KEY (id),
    CONSTRAINT fk_trabajos_realizados_orden FOREIGN KEY (orden_id) REFERENCES ordenes_mantenimiento (id),
    CONSTRAINT fk_trabajos_realizados_repuesto FOREIGN KEY (repuesto_id) REFERENCES repuestos (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX ix_unidades_situacion ON unidades (situacion_operativa);
CREATE INDEX ix_documentos_vehiculares_unidad ON documentos_vehiculares (unidad_id);
CREATE INDEX ix_documentos_vehiculares_tipo ON documentos_vehiculares (tipo_documento);
CREATE INDEX ix_repuestos_codigo ON repuestos (codigo);
CREATE INDEX ix_ordenes_mantenimiento_unidad ON ordenes_mantenimiento (unidad_id);
CREATE INDEX ix_ordenes_mantenimiento_estado ON ordenes_mantenimiento (estado);
CREATE INDEX ix_trabajos_realizados_orden ON trabajos_realizados (orden_id);
CREATE INDEX ix_trabajos_realizados_repuesto ON trabajos_realizados (repuesto_id);
