-- Slice S4-api-interna.

CREATE TABLE peticiones_idempotentes (
    clave         VARCHAR(200) NOT NULL,
    recurso_id    VARCHAR(40)  NOT NULL,
    registrada_en DATETIME(6)  NOT NULL,
    CONSTRAINT pk_peticiones_idempotentes PRIMARY KEY (clave)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
