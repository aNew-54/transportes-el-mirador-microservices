-- El snapshot del contrato 9 pasa a llevar la condicion de pago de la orden. De ella depende el
-- contrato 10: solo las facturas a credito entran a la cartera de Cobranza.
--
-- El DEFAULT existe para que el ALTER pueda rellenar las filas que ya estuvieran escritas, y se
-- retira acto seguido. Dejarlo puesto convertiria en CONTADO cualquier insercion que olvidara la
-- columna, que es exactamente el atajo que se quito del constructor de SnapshotComercial: una
-- factura a credito registrada como contado nunca llega a la cartera y nadie la reclama.
ALTER TABLE facturas ADD COLUMN snapshot_condicion_modalidad VARCHAR(20) NOT NULL DEFAULT 'CONTADO';
ALTER TABLE facturas ADD COLUMN snapshot_condicion_plazo INT NOT NULL DEFAULT 0;

ALTER TABLE facturas ALTER COLUMN snapshot_condicion_modalidad DROP DEFAULT;
ALTER TABLE facturas ALTER COLUMN snapshot_condicion_plazo DROP DEFAULT;
