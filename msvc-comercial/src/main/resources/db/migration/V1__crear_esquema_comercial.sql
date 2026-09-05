-- Contexto: Gestion Comercial. Esquema mirador_comercial.
--
-- El esquema lo versiona Flyway, nunca Hibernate: con ddl-auto=validate una entidad sin su
-- migracion rompe el build, y es deliberado.
--
-- Las claves foraneas viven SOLO dentro del contexto y dentro del agregado. Las referencias a otros
-- contextos o que crucen agregados (cliente_id, contrato_id, tarifario_id) son identificadores escalares sin FK.

CREATE TABLE clientes (
    id                             VARCHAR(40)  NOT NULL,
    ruc                            VARCHAR(11)  NOT NULL,
    razon_social                   VARCHAR(200) NOT NULL,
    condicion_habitual_modalidad   VARCHAR(10)  NOT NULL,
    condicion_habitual_plazo_dias  INT          NOT NULL,
    estado_crediticio_situacion    VARCHAR(20)  NOT NULL,
    estado_crediticio_fecha_cambio DATE         NOT NULL,
    CONSTRAINT pk_clientes PRIMARY KEY (id),
    CONSTRAINT uq_clientes_ruc UNIQUE (ruc)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE cotizaciones (
    id                              VARCHAR(40)   NOT NULL,
    cliente_id                      VARCHAR(40)   NOT NULL,
    tarifario_id                    VARCHAR(40)   NOT NULL,
    carga_peso_kg                   INT           NOT NULL,
    carga_volumen_m3                DECIMAL(10,2) NOT NULL,
    carga_tipo                      VARCHAR(30)   NOT NULL,
    ruta_origen                     VARCHAR(100)  NOT NULL,
    ruta_destino                    VARCHAR(100)  NOT NULL,
    ruta_corredor                   VARCHAR(100)  NOT NULL,
    tarifa_base_monto               DECIMAL(15,2) NOT NULL,
    tarifa_base_moneda              VARCHAR(3)    NOT NULL,
    tarifa_descuento_porcentaje     DECIMAL(5,2)  NULL,
    tarifa_descuento_autorizado_por VARCHAR(100)  NULL,
    -- Los recargos de una tarifa se guardan serializados: son parte de su valor y no se
    -- consultan por separado. Ver RecargosConverter y por que no es una @ElementCollection.
    tarifa_recargos                 VARCHAR(500)  NOT NULL,
    vigencia_desde                  DATE          NOT NULL,
    vigencia_hasta                  DATE          NOT NULL,
    estado                          VARCHAR(20)   NOT NULL,
    motivo_de_rechazo               VARCHAR(20)   NULL,
    CONSTRAINT pk_cotizaciones PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE ordenes_de_servicio (
    id                                   VARCHAR(40)   NOT NULL,
    cliente_id                           VARCHAR(40)   NOT NULL,
    contrato_id                          VARCHAR(40)   NULL,
    carga_peso_kg                        INT           NOT NULL,
    carga_volumen_m3                     DECIMAL(10,2) NOT NULL,
    carga_tipo                           VARCHAR(30)   NOT NULL,
    ruta_origen                          VARCHAR(100)  NOT NULL,
    ruta_destino                         VARCHAR(100)  NOT NULL,
    ruta_corredor                        VARCHAR(100)  NOT NULL,
    tarifa_base_monto                    DECIMAL(15,2) NOT NULL,
    tarifa_base_moneda                   VARCHAR(3)    NOT NULL,
    tarifa_descuento_porcentaje          DECIMAL(5,2)  NULL,
    tarifa_descuento_autorizado_por      VARCHAR(100)  NULL,
    tarifa_recargos                      VARCHAR(500)  NOT NULL,
    condicion_pago_modalidad             VARCHAR(10)   NOT NULL,
    condicion_pago_plazo_dias            INT           NOT NULL,
    estado                               VARCHAR(20)   NOT NULL,
    falso_flete_base_monto               DECIMAL(15,2) NULL,
    falso_flete_base_moneda              VARCHAR(3)    NULL,
    falso_flete_descuento_porcentaje     DECIMAL(5,2)  NULL,
    falso_flete_descuento_autorizado_por VARCHAR(100)  NULL,
    falso_flete_recargos                 VARCHAR(500)  NULL,
    cancelado_por                        VARCHAR(100)  NULL,
    CONSTRAINT pk_ordenes_de_servicio PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE contratos_marco (
    id                      VARCHAR(40) NOT NULL,
    cliente_id              VARCHAR(40) NOT NULL,
    vigencia_desde          DATE        NOT NULL,
    vigencia_hasta          DATE        NOT NULL,
    tiempo_libre_horas      INT         NOT NULL,
    consolidacion_permitida BIT(1)      NOT NULL,
    CONSTRAINT pk_contratos_marco PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE contrato_marco_restricciones (
    contrato_marco_id VARCHAR(40)  NOT NULL,
    corredor          VARCHAR(100) NOT NULL,
    CONSTRAINT fk_restricciones_contrato FOREIGN KEY (contrato_marco_id) REFERENCES contratos_marco (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE tarifas_pactadas (
    id                VARCHAR(40)   NOT NULL,
    contrato_marco_id VARCHAR(40)   NOT NULL,
    ruta_origen       VARCHAR(100)  NOT NULL,
    ruta_destino      VARCHAR(100)  NOT NULL,
    ruta_corredor     VARCHAR(100)  NOT NULL,
    tipo_unidad       VARCHAR(20)   NOT NULL,
    precio_monto      DECIMAL(15,2) NOT NULL,
    precio_moneda     VARCHAR(3)    NOT NULL,
    CONSTRAINT pk_tarifas_pactadas PRIMARY KEY (id),
    CONSTRAINT fk_tarifas_pactadas_contrato FOREIGN KEY (contrato_marco_id) REFERENCES contratos_marco (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE tarifarios (
    id             VARCHAR(40) NOT NULL,
    vigencia_desde DATE        NOT NULL,
    vigencia_hasta DATE        NOT NULL,
    CONSTRAINT pk_tarifarios PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE precios_de_tarifario (
    id            VARCHAR(40)   NOT NULL,
    tarifario_id  VARCHAR(40)   NOT NULL,
    ruta_origen   VARCHAR(100)  NOT NULL,
    ruta_destino  VARCHAR(100)  NOT NULL,
    ruta_corredor VARCHAR(100)  NOT NULL,
    tipo_unidad   VARCHAR(20)   NOT NULL,
    precio_monto  DECIMAL(15,2) NOT NULL,
    precio_moneda VARCHAR(3)    NOT NULL,
    CONSTRAINT pk_precios_de_tarifario PRIMARY KEY (id),
    CONSTRAINT fk_precios_tarifario FOREIGN KEY (tarifario_id) REFERENCES tarifarios (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE tarifario_recargos (
    tarifario_id VARCHAR(40)  NOT NULL,
    tipo         VARCHAR(30)  NOT NULL,
    porcentaje   DECIMAL(5,2) NOT NULL,
    CONSTRAINT fk_tarifario_recargos_tarifario FOREIGN KEY (tarifario_id) REFERENCES tarifarios (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Indices para busqueda y optimizacion de consultas derivadas
CREATE INDEX ix_clientes_estado_crediticio ON clientes (estado_crediticio_situacion);
CREATE INDEX ix_cotizaciones_cliente ON cotizaciones (cliente_id);
CREATE INDEX ix_cotizaciones_estado ON cotizaciones (estado);
CREATE INDEX ix_ordenes_de_servicio_cliente ON ordenes_de_servicio (cliente_id);
CREATE INDEX ix_ordenes_de_servicio_estado ON ordenes_de_servicio (estado);
CREATE INDEX ix_contratos_marco_cliente ON contratos_marco (cliente_id);
CREATE INDEX ix_restricciones_contrato ON contrato_marco_restricciones (contrato_marco_id);
CREATE INDEX ix_tarifas_pactadas_contrato ON tarifas_pactadas (contrato_marco_id);
CREATE INDEX ix_precios_tarifario ON precios_de_tarifario (tarifario_id);
CREATE INDEX ix_tarifario_recargos_tarifario ON tarifario_recargos (tarifario_id);

-- TAR-01: indice que ayuda a detectar dos tarifarios vigentes solapados
CREATE INDEX ix_tarifarios_vigencia ON tarifarios (vigencia_desde, vigencia_hasta);
