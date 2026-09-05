-- Contexto: Programacion y Despacho. Esquema mirador_programacion.
--
-- El esquema lo versiona Flyway, nunca Hibernate: con ddl-auto=validate una entidad sin su
-- migracion rompe el build, y es deliberado.
--
-- Las claves foraneas viven SOLO dentro del agregado. Las referencias a otros contextos
-- (unidad_id, conductor_id, orden_de_servicio_id) son identificadores escalares sin FK.

CREATE TABLE viajes (
    id            VARCHAR(40)   NOT NULL,
    ruta_origen   VARCHAR(100)  NOT NULL,
    ruta_destino  VARCHAR(100)  NOT NULL,
    ruta_corredor VARCHAR(50)   NOT NULL,
    ventana_desde DATETIME(6)   NOT NULL,
    ventana_hasta DATETIME(6)   NOT NULL,
    estado        VARCHAR(20)   NOT NULL,
    unidad_id     VARCHAR(40)   NULL,
    con_relevo    BIT(1)        NULL,
    CONSTRAINT pk_viajes PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE viaje_ordenes (
    viaje_id VARCHAR(40) NOT NULL,
    orden_id VARCHAR(40) NOT NULL,
    CONSTRAINT fk_viaje_ordenes_viaje FOREIGN KEY (viaje_id) REFERENCES viajes (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE viaje_conductores (
    viaje_id     VARCHAR(40) NOT NULL,
    conductor_id VARCHAR(40) NOT NULL,
    CONSTRAINT fk_viaje_conductores_viaje FOREIGN KEY (viaje_id) REFERENCES viajes (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE viaje_cargas (
    viaje_id              VARCHAR(40)   NOT NULL,
    orden_de_servicio_id  VARCHAR(40)   NOT NULL,
    peso_kg               INT           NOT NULL,
    volumen_m3            DECIMAL(10,2) NOT NULL,
    tipo                  VARCHAR(20)   NOT NULL,
    secuencia_de_descarga INT           NOT NULL,
    CONSTRAINT fk_viaje_cargas_viaje FOREIGN KEY (viaje_id) REFERENCES viajes (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE viaje_paradas (
    viaje_id             VARCHAR(40)  NOT NULL,
    secuencia            INT          NOT NULL,
    tipo                 VARCHAR(20)  NOT NULL,
    orden_de_servicio_id VARCHAR(40)  NOT NULL,
    ubicacion            VARCHAR(300) NULL,
    hora_estimada        DATETIME(6)  NULL,
    CONSTRAINT fk_viaje_paradas_viaje FOREIGN KEY (viaje_id) REFERENCES viajes (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE agendas_unidades (
    unidad_id VARCHAR(40) NOT NULL,
    CONSTRAINT pk_agendas_unidades PRIMARY KEY (unidad_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE reservas_unidades (
    id            VARCHAR(40) NOT NULL,
    unidad_id     VARCHAR(40) NOT NULL,
    viaje_id      VARCHAR(40) NOT NULL,
    ventana_desde DATETIME(6) NOT NULL,
    ventana_hasta DATETIME(6) NOT NULL,
    estado        VARCHAR(20) NOT NULL,
    CONSTRAINT pk_reservas_unidades PRIMARY KEY (id),
    CONSTRAINT fk_reservas_unidades_agenda FOREIGN KEY (unidad_id) REFERENCES agendas_unidades (unidad_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE agendas_conductores (
    conductor_id VARCHAR(40) NOT NULL,
    CONSTRAINT pk_agendas_conductores PRIMARY KEY (conductor_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE reservas_conductores (
    id            VARCHAR(40) NOT NULL,
    conductor_id  VARCHAR(40) NOT NULL,
    viaje_id      VARCHAR(40) NOT NULL,
    ventana_desde DATETIME(6) NOT NULL,
    ventana_hasta DATETIME(6) NOT NULL,
    estado        VARCHAR(20) NOT NULL,
    CONSTRAINT pk_reservas_conductores PRIMARY KEY (id),
    CONSTRAINT fk_reservas_conductores_agenda FOREIGN KEY (conductor_id) REFERENCES agendas_conductores (conductor_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX ix_viajes_estado ON viajes (estado);
CREATE INDEX ix_viajes_corredor ON viajes (ruta_corredor);
CREATE INDEX ix_viajes_unidad ON viajes (unidad_id);
CREATE INDEX ix_viaje_ordenes_viaje ON viaje_ordenes (viaje_id);
CREATE INDEX ix_viaje_conductores_viaje ON viaje_conductores (viaje_id);
CREATE INDEX ix_viaje_cargas_viaje ON viaje_cargas (viaje_id);
CREATE INDEX ix_viaje_paradas_viaje ON viaje_paradas (viaje_id);
CREATE INDEX ix_reservas_unidades_solape ON reservas_unidades (unidad_id, ventana_desde, ventana_hasta);
CREATE INDEX ix_reservas_conductores_solape ON reservas_conductores (conductor_id, ventana_desde, ventana_hasta);
CREATE INDEX ix_reservas_unidades_viaje ON reservas_unidades (viaje_id);
CREATE INDEX ix_reservas_conductores_viaje ON reservas_conductores (viaje_id);
