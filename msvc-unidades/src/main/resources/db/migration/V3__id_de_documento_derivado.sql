-- El id de un DocumentoVehicular es derivado: `<unidadId>-<TIPO_DOCUMENTO>`. Deliberado, porque asi
-- registrar dos veces el mismo tipo sobre la misma unidad reemplaza en vez de duplicar.
--
-- Pero el id de la unidad es un UUID de 36 caracteres, y tipo_documento es VARCHAR(30): el id
-- derivado llega a 67 y la columna tenia 40. En produccion eso significaba que NINGUNA unidad podia
-- registrar un documento —el insert moria con «Data too long for column 'id'» y salia un 500 sin
-- problem+json— y por tanto ninguna unidad podia llegar a ser elegible, porque la elegibilidad
-- exige SOAT vigente. Las pruebas no lo veian porque construian los documentos con ids cortos
-- puestos a mano en vez de pasar por el camino que los deriva.
ALTER TABLE documentos_vehiculares MODIFY COLUMN id VARCHAR(80) NOT NULL;
