-- Slice S4-api-interna. Lo que los contratos 1 y 7 piden y el dominio no guardaba.
--
-- El esquema lo versiona Flyway, nunca Hibernate: con ddl-auto=validate una entidad sin su
-- migracion rompe el build, y es deliberado.

-- Contrato 1. Cinco campos que la orden no tenia y que Programacion necesita:
--   - embalaje y naturaleza de la carga, que el contrato 7 ademas compara entre lo declarado y lo real
--   - la distancia de la ruta, que es lo que se cobra
--   - la ventana de servicio, sin la cual Programacion no puede comprobar VIA-03
--   - el tipo de unidad requerido, que ya se usaba para buscar la tarifa pactada y no se guardaba
--
-- Todos NULL: hay ordenes anteriores a este slice que no los tienen. La API los exige al crear.
ALTER TABLE ordenes_de_servicio
    ADD COLUMN carga_embalaje        VARCHAR(50)  NULL,
    ADD COLUMN carga_naturaleza      VARCHAR(50)  NULL,
    ADD COLUMN ruta_distancia_km     INT          NULL,
    ADD COLUMN ventana_inicio        DATETIME(6)  NULL,
    ADD COLUMN ventana_fin           DATETIME(6)  NULL,
    ADD COLUMN tipo_unidad_requerido VARCHAR(20)  NULL;

-- `Carga` es un objeto de valor embebido tambien en la cotizacion. Con ddl-auto=validate, un campo
-- nuevo en un embebido exige su columna en TODAS las tablas donde ese embebido vive, y con el nombre
-- que diga su @AttributeOverride: sin override, Hibernate pide la columna `embalaje` a secas. La
-- validacion de esquema lo caza en el arranque, que es justamente para lo que esta.
--
-- La distancia NO entra en `Ruta`. Una ruta es su origen, su destino y su corredor: eso es lo que la
-- identifica y es como se busca la tarifa pactada. Anadirle la distancia le cambia la identidad, y dos
-- rutas iguales medidas distinto dejan de encontrarse. Vive en la orden, que es de quien es.
ALTER TABLE cotizaciones
    ADD COLUMN carga_embalaje   VARCHAR(50) NULL,
    ADD COLUMN carga_naturaleza VARCHAR(50) NULL;

-- Contrato 7. La espera facturable que Ejecucion reporta no tenia donde ir: la primera version del
-- slice guardaba la clave de idempotencia y descartaba el dato, de modo que la proteccion contra
-- duplicados protegia un efecto que no existia.
--
-- viaje_id es un identificador escalar sin FK: el viaje vive en msvc-programacion.
CREATE TABLE esperas_registradas (
    id                 VARCHAR(40)  NOT NULL,
    orden_id           VARCHAR(40)  NOT NULL,
    viaje_id           VARCHAR(40)  NOT NULL,
    punto              VARCHAR(20)  NOT NULL,
    tiempo_libre_horas DECIMAL(6,2) NOT NULL,
    tiempo_real_horas  DECIMAL(6,2) NOT NULL,
    excedente_horas    DECIMAL(6,2) NOT NULL,
    CONSTRAINT pk_esperas_registradas PRIMARY KEY (id),
    CONSTRAINT fk_esperas_registradas_orden FOREIGN KEY (orden_id) REFERENCES ordenes_de_servicio (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX ix_esperas_registradas_orden ON esperas_registradas (orden_id);
CREATE INDEX ix_esperas_registradas_viaje ON esperas_registradas (viaje_id);
