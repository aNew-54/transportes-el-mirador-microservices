-- Regla 6 de los contratos: un reintento con la misma Idempotency-Key devuelve el resultado
-- original y no repite el efecto. La clave la construye el consumidor; aqui se guarda tal cual.
-- La escritura del efecto y el registro de la clave van en la misma transaccion.
CREATE TABLE peticiones_idempotentes (
    clave         VARCHAR(200) NOT NULL,
    recurso_id    VARCHAR(40)  NOT NULL,
    registrada_en DATETIME(6)  NOT NULL,
    CONSTRAINT pk_peticiones_idempotentes PRIMARY KEY (clave)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
