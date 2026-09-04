-- Contexto: Gestion de Conductores. Esquema mirador_conductores.
--
-- El esquema lo versiona Flyway, nunca Hibernate: con ddl-auto=validate una entidad sin su
-- migracion rompe el build, y es deliberado.
--
-- Las claves foraneas viven SOLO dentro del contexto. Las referencias a otros contextos
-- (cliente_id) son identificadores escalares sin FK: el cliente vive en msvc-comercial.

CREATE TABLE conductores (
    id                  VARCHAR(40)   NOT NULL,
    nombre_completo     VARCHAR(200)  NOT NULL,
    numero_licencia     VARCHAR(9)    NOT NULL,
    categoria_licencia  VARCHAR(10)   NOT NULL,
    licencia_desde      DATE          NOT NULL,
    licencia_hasta      DATE          NOT NULL,
    horas_acumuladas    DECIMAL(5,2)  NOT NULL,
    ventana_desde       DATE          NOT NULL,
    ventana_hasta       DATE          NOT NULL,
    situacion           VARCHAR(20)   NOT NULL,
    motivo_habilitacion VARCHAR(300)  NULL,
    CONSTRAINT pk_conductores PRIMARY KEY (id),
    CONSTRAINT uq_conductores_licencia UNIQUE (numero_licencia)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE inducciones (
    id            VARCHAR(40) NOT NULL,
    conductor_id  VARCHAR(40) NOT NULL,
    cliente_id    VARCHAR(40) NOT NULL,
    vigente_desde DATE        NOT NULL,
    vigente_hasta DATE        NOT NULL,
    CONSTRAINT pk_inducciones PRIMARY KEY (id),
    CONSTRAINT fk_inducciones_conductor FOREIGN KEY (conductor_id) REFERENCES conductores (id),
    -- CON-03 se evalua por cliente: un conductor tiene como mucho una induccion vigente por cliente.
    CONSTRAINT uq_inducciones_conductor_cliente UNIQUE (conductor_id, cliente_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX ix_conductores_situacion ON conductores (situacion);
CREATE INDEX ix_inducciones_cliente ON inducciones (cliente_id);
