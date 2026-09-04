-- Contexto: Cobranza. Esquema mirador_cobranza.
--
-- El esquema lo versiona Flyway, nunca Hibernate: con ddl-auto=validate una entidad sin su
-- migracion rompe el build, y es deliberado.
--
-- Las claves foraneas viven SOLO dentro del contexto. Las referencias a otros contextos
-- (factura_id, documento_id, cuenta_por_cobrar_id) son identificadores escalares sin FK:
-- la factura vive en msvc-facturacion.
-- El cliente_id dentro de pagos va sin FK aunque coincida con la identidad de la cuenta corriente:
-- son agregados distintos, y una FK entre agregados los acopla.

CREATE TABLE cuentas_corrientes (
    cliente_id      VARCHAR(40)  NOT NULL,
    situacion       VARCHAR(20)  NOT NULL,
    motivo          VARCHAR(300) NULL,
    fecha_de_cambio DATE         NOT NULL,
    CONSTRAINT pk_cuentas_corrientes PRIMARY KEY (cliente_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE cuentas_por_cobrar (
    id                    VARCHAR(40)   NOT NULL,
    cliente_id            VARCHAR(40)   NOT NULL,
    factura_id            VARCHAR(40)   NOT NULL,
    documento_id          VARCHAR(40)   NOT NULL,
    total_monto           DECIMAL(15,2) NOT NULL,
    total_moneda          VARCHAR(3)    NOT NULL,
    detraccion_monto      DECIMAL(15,2) NOT NULL,
    detraccion_moneda     VARCHAR(3)    NOT NULL,
    fecha_de_vencimiento  DATE          NOT NULL,
    aplicado_monto        DECIMAL(15,2) NOT NULL,
    aplicado_moneda       VARCHAR(3)    NOT NULL,
    detraccion_depositada BOOLEAN       NOT NULL,
    CONSTRAINT pk_cuentas_por_cobrar PRIMARY KEY (id),
    CONSTRAINT fk_cuentas_por_cobrar_cuenta FOREIGN KEY (cliente_id) REFERENCES cuentas_corrientes (cliente_id),
    -- Idempotencia del contrato 10: una factura solo puede registrarse una vez en la cartera
    CONSTRAINT uq_cuentas_por_cobrar_factura UNIQUE (factura_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE pagos (
    id           VARCHAR(40)   NOT NULL,
    cliente_id   VARCHAR(40)   NOT NULL,
    monto_monto  DECIMAL(15,2) NOT NULL,
    monto_moneda VARCHAR(3)    NOT NULL,
    modalidad    VARCHAR(20)   NOT NULL,
    referencia   VARCHAR(100)  NULL,
    fecha        DATE          NOT NULL,
    CONSTRAINT pk_pagos PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE aplicaciones (
    id                   VARCHAR(40)   NOT NULL,
    pago_id              VARCHAR(40)   NOT NULL,
    cuenta_por_cobrar_id VARCHAR(40)   NOT NULL,
    importe_monto        DECIMAL(15,2) NOT NULL,
    importe_moneda       VARCHAR(3)    NOT NULL,
    CONSTRAINT pk_aplicaciones PRIMARY KEY (id),
    CONSTRAINT fk_aplicaciones_pago FOREIGN KEY (pago_id) REFERENCES pagos (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX ix_cuentas_corrientes_situacion ON cuentas_corrientes (situacion);
CREATE INDEX ix_cuentas_por_cobrar_cliente ON cuentas_por_cobrar (cliente_id);
CREATE INDEX ix_cuentas_por_cobrar_vencimiento ON cuentas_por_cobrar (fecha_de_vencimiento);
CREATE INDEX ix_pagos_cliente ON pagos (cliente_id);
CREATE INDEX ix_pagos_fecha ON pagos (fecha);
CREATE INDEX ix_aplicaciones_pago ON aplicaciones (pago_id);
CREATE INDEX ix_aplicaciones_cuenta ON aplicaciones (cuenta_por_cobrar_id);
