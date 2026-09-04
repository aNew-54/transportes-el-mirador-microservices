-- Contexto: Facturacion. Esquema mirador_facturacion.
--
-- El esquema lo versiona Flyway, nunca Hibernate. Las claves foraneas viven solo dentro del
-- agregado: entre facturas y sus lineas si, entre facturas y notas de credito NO, porque son
-- dos raices de agregado distintas y una FK las acoplaria.

CREATE TABLE facturas (
    id                       VARCHAR(40)    NOT NULL,
    orden_de_servicio_id     VARCHAR(40)    NOT NULL,
    cliente_id               VARCHAR(40)    NOT NULL,
    comprobante_serie        VARCHAR(4)     NULL,
    comprobante_correlativo  INT            NULL,
    snapshot_orden_id        VARCHAR(40)    NOT NULL,
    snapshot_cliente_id      VARCHAR(40)    NOT NULL,
    snapshot_tarifa_monto    DECIMAL(15,2)  NOT NULL,
    snapshot_tarifa_moneda   VARCHAR(3)     NOT NULL,
    snapshot_moneda          VARCHAR(3)     NOT NULL,
    snapshot_obtenido_en     DATETIME(6)    NOT NULL,
    detraccion_porcentaje    DECIMAL(5,2)   NOT NULL,
    detraccion_monto         DECIMAL(15,2)  NOT NULL,
    detraccion_moneda        VARCHAR(3)     NOT NULL,
    detraccion_cuenta        VARCHAR(40)    NULL,
    conformidad_registrada   BIT(1)         NOT NULL,
    conformidad_recibida_en  DATETIME(6)    NULL,
    estado                   VARCHAR(20)    NOT NULL,
    fecha_de_emision         DATETIME(6)    NULL,
    falso_flete              BIT(1)         NOT NULL,
    CONSTRAINT pk_facturas PRIMARY KEY (id),
    -- FAC-02: una factura corresponde a exactamente una orden de servicio.
    CONSTRAINT uq_facturas_orden UNIQUE (orden_de_servicio_id),
    -- El numero de comprobante no admite saltos ni repeticiones.
    CONSTRAINT uq_facturas_comprobante UNIQUE (comprobante_serie, comprobante_correlativo)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE lineas_de_factura (
    id                   VARCHAR(40)   NOT NULL,
    factura_id           VARCHAR(40)   NOT NULL,
    orden_de_servicio_id VARCHAR(40)   NOT NULL,
    concepto             VARCHAR(20)   NOT NULL,
    descripcion          VARCHAR(300)  NOT NULL,
    importe_monto        DECIMAL(15,2) NOT NULL,
    importe_moneda       VARCHAR(3)    NOT NULL,
    CONSTRAINT pk_lineas_de_factura PRIMARY KEY (id),
    CONSTRAINT fk_lineas_factura FOREIGN KEY (factura_id) REFERENCES facturas (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Incidencias sin resolver que bloquean la emision (FAC-05). Viven dentro del objeto de valor
-- Conformidad, que es un record: por eso se cargan EAGER, para poder construirlo.
CREATE TABLE factura_incidencias (
    factura_id VARCHAR(40)  NOT NULL,
    incidencia VARCHAR(200) NOT NULL,
    CONSTRAINT fk_incidencias_factura FOREIGN KEY (factura_id) REFERENCES facturas (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Importes ya ajustados por notas de credito. La factura guarda el importe, no la nota:
-- NotaDeCredito es otra raiz de agregado.
CREATE TABLE factura_ajustes (
    factura_id    VARCHAR(40)   NOT NULL,
    monto         DECIMAL(15,2) NOT NULL,
    codigo_moneda VARCHAR(3)    NOT NULL,
    CONSTRAINT fk_ajustes_factura FOREIGN KEY (factura_id) REFERENCES facturas (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE notas_de_credito (
    id               VARCHAR(40)   NOT NULL,
    factura_id       VARCHAR(40)   NOT NULL,
    motivo           VARCHAR(30)   NOT NULL,
    monto            DECIMAL(15,2) NOT NULL,
    codigo_moneda    VARCHAR(3)    NOT NULL,
    fecha_de_emision DATETIME(6)   NOT NULL,
    motivo_detalle   VARCHAR(300)  NULL,
    CONSTRAINT pk_notas_de_credito PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX ix_facturas_estado ON facturas (estado);
CREATE INDEX ix_facturas_cliente ON facturas (cliente_id);
CREATE INDEX ix_notas_factura ON notas_de_credito (factura_id);
