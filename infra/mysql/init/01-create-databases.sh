#!/bin/sh
set -e

mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE DATABASE IF NOT EXISTS mirador_comercial CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS mirador_programacion CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS mirador_ejecucion CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS mirador_unidades CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS mirador_conductores CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS mirador_facturacion CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS mirador_cobranza CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'mirador_comercial'@'%' IDENTIFIED BY '${COMERCIAL_DB_PASSWORD}';
CREATE USER IF NOT EXISTS 'mirador_programacion'@'%' IDENTIFIED BY '${PROGRAMACION_DB_PASSWORD}';
CREATE USER IF NOT EXISTS 'mirador_ejecucion'@'%' IDENTIFIED BY '${EJECUCION_DB_PASSWORD}';
CREATE USER IF NOT EXISTS 'mirador_unidades'@'%' IDENTIFIED BY '${UNIDADES_DB_PASSWORD}';
CREATE USER IF NOT EXISTS 'mirador_conductores'@'%' IDENTIFIED BY '${CONDUCTORES_DB_PASSWORD}';
CREATE USER IF NOT EXISTS 'mirador_facturacion'@'%' IDENTIFIED BY '${FACTURACION_DB_PASSWORD}';
CREATE USER IF NOT EXISTS 'mirador_cobranza'@'%' IDENTIFIED BY '${COBRANZA_DB_PASSWORD}';

ALTER USER 'mirador_comercial'@'%' IDENTIFIED BY '${COMERCIAL_DB_PASSWORD}';
ALTER USER 'mirador_programacion'@'%' IDENTIFIED BY '${PROGRAMACION_DB_PASSWORD}';
ALTER USER 'mirador_ejecucion'@'%' IDENTIFIED BY '${EJECUCION_DB_PASSWORD}';
ALTER USER 'mirador_unidades'@'%' IDENTIFIED BY '${UNIDADES_DB_PASSWORD}';
ALTER USER 'mirador_conductores'@'%' IDENTIFIED BY '${CONDUCTORES_DB_PASSWORD}';
ALTER USER 'mirador_facturacion'@'%' IDENTIFIED BY '${FACTURACION_DB_PASSWORD}';
ALTER USER 'mirador_cobranza'@'%' IDENTIFIED BY '${COBRANZA_DB_PASSWORD}';

GRANT ALL PRIVILEGES ON mirador_comercial.* TO 'mirador_comercial'@'%';
GRANT ALL PRIVILEGES ON mirador_programacion.* TO 'mirador_programacion'@'%';
GRANT ALL PRIVILEGES ON mirador_ejecucion.* TO 'mirador_ejecucion'@'%';
GRANT ALL PRIVILEGES ON mirador_unidades.* TO 'mirador_unidades'@'%';
GRANT ALL PRIVILEGES ON mirador_conductores.* TO 'mirador_conductores'@'%';
GRANT ALL PRIVILEGES ON mirador_facturacion.* TO 'mirador_facturacion'@'%';
GRANT ALL PRIVILEGES ON mirador_cobranza.* TO 'mirador_cobranza'@'%';

FLUSH PRIVILEGES;
SQL
