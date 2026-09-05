-- Slice S4-api-interna. Lo que el contrato 4 pide y el dominio no guardaba.
--
-- El esquema lo versiona Flyway, nunca Hibernate: con ddl-auto=validate una entidad sin su
-- migracion rompe el build, y es deliberado.

-- La parada guardaba la ubicacion como un texto suelto de 300 caracteres. El contrato 4 pide
-- direccion, distrito, referencia y contacto, y con razon: un conductor necesita el distrito para
-- llegar, la referencia para encontrar la puerta y el contacto para que se la abran. La forma pobre
-- no daba para entregar.
--
-- Lo que hubiera en `ubicacion` pasa a `ubicacion_direccion`, que es lo que era.
ALTER TABLE viaje_paradas
    CHANGE COLUMN ubicacion ubicacion_direccion VARCHAR(300) NULL,
    ADD COLUMN ubicacion_distrito   VARCHAR(100) NULL AFTER ubicacion_direccion,
    ADD COLUMN ubicacion_referencia VARCHAR(200) NULL AFTER ubicacion_distrito,
    ADD COLUMN ubicacion_contacto   VARCHAR(50)  NULL AFTER ubicacion_referencia;

-- Instrucciones de la programacion para quien ejecuta. Opcional: la mayoria de los viajes no
-- necesita ninguna.
ALTER TABLE viajes
    ADD COLUMN hoja_observaciones VARCHAR(500) NULL;
