-- Slice S6-cierre. Lo que el agregado necesitaba llevar para poder rendir cuentas al cerrar.

-- Los conductores del contrato 4. Llegaban en la hoja de ruta desde S5 y se descartaban, asi que
-- el contrato 6 no tenia a quien reportarle horas.
CREATE TABLE ejecucion_conductores (
    ejecucion_id VARCHAR(40) NOT NULL,
    conductor_id VARCHAR(40) NOT NULL,
    CONSTRAINT fk_ejecucion_conductores_ejecucion FOREIGN KEY (ejecucion_id) REFERENCES ejecuciones (viaje_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Odometro al cerrar. NULL mientras el viaje no se cierre: no hay valor por defecto honesto para
-- un kilometraje que todavia no se ha leido del tablero.
ALTER TABLE ejecuciones ADD COLUMN kilometraje_final INT NULL;
