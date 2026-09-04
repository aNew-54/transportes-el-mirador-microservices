package pe.edu.unc.elmirador.facturacion.models.entity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import pe.edu.unc.elmirador.facturacion.exceptions.DominioFacturacionException;
import pe.edu.unc.elmirador.facturacion.exceptions.EmisionSinConformidadException;
import pe.edu.unc.elmirador.facturacion.exceptions.FacturaInmutableException;
import pe.edu.unc.elmirador.facturacion.exceptions.ImportesInconsistentesException;
import pe.edu.unc.elmirador.facturacion.exceptions.IncidenciaSinResolverException;
import pe.edu.unc.elmirador.facturacion.exceptions.MonedaIncompatibleException;
import pe.edu.unc.elmirador.facturacion.exceptions.MontoExcedeElSaldoException;
import pe.edu.unc.elmirador.facturacion.models.vo.ConceptoFacturable;
import pe.edu.unc.elmirador.facturacion.models.vo.Conformidad;
import pe.edu.unc.elmirador.facturacion.models.vo.Detraccion;
import pe.edu.unc.elmirador.facturacion.models.vo.EstadoDeFactura;
import pe.edu.unc.elmirador.facturacion.models.vo.NumeroDeComprobante;
import pe.edu.unc.elmirador.facturacion.models.vo.SnapshotComercial;
import pe.edu.unc.elmirador.facturacion.models.vo.Dinero;

/**
 * Raiz del agregado Factura.
 * Controla el ciclo de vida de la factura electronica, la consistencia de importes,
 * las detracciones y la aplicacion de notas de credito.
 */
public class Factura {

    private final String id;
    private final String ordenDeServicioId;
    private final String clienteId;
    private NumeroDeComprobante numeroDeComprobante;
    private final SnapshotComercial snapshotComercial;
    private final Detraccion detraccion;
    private Conformidad conformidad;
    private EstadoDeFactura estado;
    private final List<LineaDeFactura> lineas;
    private OffsetDateTime fechaDeEmision;
    private boolean falsoFlete;
    private final List<NotaDeCredito> notasDeCredito;

    private Factura(
        String id,
        String ordenDeServicioId,
        String clienteId,
        SnapshotComercial snapshotComercial,
        Detraccion detraccion,
        boolean falsoFlete
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id de la factura es obligatorio");
        }
        if (ordenDeServicioId == null || ordenDeServicioId.isBlank()) {
            throw new IllegalArgumentException("El ordenDeServicioId es obligatorio");
        }
        if (clienteId == null || clienteId.isBlank()) {
            throw new IllegalArgumentException("El clienteId es obligatorio");
        }
        if (snapshotComercial == null) {
            throw new IllegalArgumentException("El snapshot comercial es obligatorio");
        }
        if (detraccion == null) {
            throw new IllegalArgumentException("La detraccion es obligatoria");
        }

        String ordenTrim = ordenDeServicioId.trim();
        String clienteTrim = clienteId.trim();

        if (!ordenTrim.equals(snapshotComercial.ordenDeServicioId())) {
            throw new IllegalArgumentException(
                "El ordenDeServicioId (" + ordenTrim + ") no coincide con el snapshot (" + snapshotComercial.ordenDeServicioId() + ")"
            );
        }
        if (!clienteTrim.equals(snapshotComercial.clienteId())) {
            throw new IllegalArgumentException(
                "El clienteId (" + clienteTrim + ") no coincide con el snapshot (" + snapshotComercial.clienteId() + ")"
            );
        }
        if (!detraccion.monto().codigoMoneda().equalsIgnoreCase(snapshotComercial.codigoMoneda())) {
            throw new MonedaIncompatibleException(
                "La moneda de la detraccion (" + detraccion.monto().codigoMoneda()
                + ") no coincide con la del snapshot (" + snapshotComercial.codigoMoneda() + ")"
            );
        }

        this.id = id.trim();
        this.ordenDeServicioId = ordenTrim;
        this.clienteId = clienteTrim;
        this.snapshotComercial = snapshotComercial;
        this.detraccion = detraccion;
        this.conformidad = Conformidad.noRegistrada();
        this.estado = EstadoDeFactura.BLOQUEADA;
        this.lineas = new ArrayList<>();
        this.notasDeCredito = new ArrayList<>();
        this.numeroDeComprobante = null;
        this.fechaDeEmision = null;
        this.falsoFlete = falsoFlete;
    }

    public static Factura abrir(
        String id,
        String ordenDeServicioId,
        String clienteId,
        SnapshotComercial snapshotComercial,
        Detraccion detraccion
    ) {
        return new Factura(id, ordenDeServicioId, clienteId, snapshotComercial, detraccion, false);
    }

    public static Factura abrir(
        String id,
        SnapshotComercial snapshotComercial,
        Detraccion detraccion
    ) {
        if (snapshotComercial == null) {
            throw new IllegalArgumentException("El snapshot comercial es obligatorio");
        }
        return abrir(id, snapshotComercial.ordenDeServicioId(), snapshotComercial.clienteId(), snapshotComercial, detraccion);
    }

    public static Factura abrirFalsoFlete(
        String id,
        String ordenDeServicioId,
        String clienteId,
        SnapshotComercial snapshotComercial,
        Detraccion detraccion
    ) {
        return new Factura(id, ordenDeServicioId, clienteId, snapshotComercial, detraccion, true);
    }

    public static Factura abrirFalsoFlete(
        String id,
        SnapshotComercial snapshotComercial,
        Detraccion detraccion
    ) {
        if (snapshotComercial == null) {
            throw new IllegalArgumentException("El snapshot comercial es obligatorio");
        }
        return abrirFalsoFlete(id, snapshotComercial.ordenDeServicioId(), snapshotComercial.clienteId(), snapshotComercial, detraccion);
    }

    public void agregarLinea(LineaDeFactura linea) {
        if (linea == null) {
            throw new IllegalArgumentException("La linea de factura es obligatoria");
        }
        if (this.estado == EstadoDeFactura.EMITIDA || this.estado == EstadoDeFactura.ANULADA) {
            throw new FacturaInmutableException(
                "No se pueden agregar lineas a una factura en estado " + this.estado
            );
        }
        if (linea.ordenDeServicioId() != null && !this.ordenDeServicioId.equals(linea.ordenDeServicioId())) {
            throw new IllegalArgumentException(
                "La linea pertenece a la orden " + linea.ordenDeServicioId()
                + " pero la factura es de la orden " + this.ordenDeServicioId
            );
        }
        if (!this.snapshotComercial.codigoMoneda().equalsIgnoreCase(linea.importe().codigoMoneda())) {
            throw new MonedaIncompatibleException(
                "La moneda de la linea (" + linea.importe().codigoMoneda()
                + ") no coincide con la de la factura (" + this.snapshotComercial.codigoMoneda() + ")"
            );
        }
        this.lineas.add(linea);
    }

    public void registrarConformidad(Conformidad conformidad) {
        if (conformidad == null) {
            throw new IllegalArgumentException("La conformidad es obligatoria");
        }
        if (this.estado == EstadoDeFactura.EMITIDA || this.estado == EstadoDeFactura.ANULADA) {
            throw new FacturaInmutableException(
                "No se puede registrar conformidad en una factura en estado " + this.estado
            );
        }
        this.conformidad = conformidad;
    }

    public Dinero total() {
        Dinero acumulado = Dinero.cero(this.snapshotComercial.codigoMoneda());
        for (LineaDeFactura linea : this.lineas) {
            acumulado = acumulado.sumar(linea.importe());
        }
        return acumulado;
    }

    public Dinero montoNeto() {
        return this.detraccion.montoNeto(total());
    }

    public Dinero saldoAjustable() {
        Dinero saldo = total();
        for (NotaDeCredito nc : this.notasDeCredito) {
            saldo = saldo.restar(nc.monto());
        }
        return saldo;
    }

    public void aplicarNotaDeCredito(NotaDeCredito notaDeCredito) {
        if (notaDeCredito == null) {
            throw new IllegalArgumentException("La nota de credito es obligatoria");
        }
        if (!this.id.equals(notaDeCredito.facturaId())) {
            throw new IllegalArgumentException(
                "La nota de credito pertenece a la factura " + notaDeCredito.facturaId()
                + " pero esta factura es " + this.id
            );
        }
        if (this.estado != EstadoDeFactura.EMITIDA) {
            throw new FacturaInmutableException(
                "Solo se pueden aplicar notas de credito a facturas emitidas. Estado actual: " + this.estado
            );
        }
        if (!notaDeCredito.monto().codigoMoneda().equalsIgnoreCase(this.snapshotComercial.codigoMoneda())) {
            throw new MonedaIncompatibleException(
                "La moneda de la nota de credito no coincide con la moneda de la factura"
            );
        }
        if (notaDeCredito.monto().esMayorQue(saldoAjustable())) {
            throw new MontoExcedeElSaldoException(
                "El monto de la nota de credito (" + notaDeCredito.monto().monto()
                + ") excede el saldo ajustable restante de la factura (" + saldoAjustable().monto() + ")"
            );
        }
        this.notasDeCredito.add(notaDeCredito);
    }

    public void emitir(NumeroDeComprobante numeroDeComprobante, OffsetDateTime fechaDeEmision) {
        if (fechaDeEmision == null) {
            throw new IllegalArgumentException("La fecha de emision es obligatoria");
        }
        if (numeroDeComprobante == null) {
            throw new IllegalArgumentException("El numero de comprobante es obligatorio");
        }
        if (this.estado == EstadoDeFactura.EMITIDA) {
            throw new FacturaInmutableException("La factura ya fue emitida y es inmutable");
        }
        if (this.estado == EstadoDeFactura.ANULADA) {
            throw new FacturaInmutableException("No se puede emitir una factura anulada");
        }
        if (!this.falsoFlete && !this.conformidad.registrada()) {
            throw new EmisionSinConformidadException(
                "No se puede emitir la factura sin conformidad de entrega registrada"
            );
        }
        if (this.conformidad.incidenciasSinResolver() != null && !this.conformidad.incidenciasSinResolver().isEmpty()) {
            throw new IncidenciaSinResolverException(
                "No se puede emitir la factura con incidencias sin resolver: " + this.conformidad.incidenciasSinResolver()
            );
        }
        if (this.lineas.isEmpty()) {
            throw new DominioFacturacionException("No se puede emitir una factura sin lineas");
        }

        validarConsistenciaDeImportes();

        this.numeroDeComprobante = numeroDeComprobante;
        this.fechaDeEmision = fechaDeEmision;
        this.estado = EstadoDeFactura.EMITIDA;
    }

    public void emitirFalsoFlete(NumeroDeComprobante numeroDeComprobante, OffsetDateTime fechaDeEmision) {
        if (fechaDeEmision == null) {
            throw new IllegalArgumentException("La fecha de emision es obligatoria");
        }
        if (numeroDeComprobante == null) {
            throw new IllegalArgumentException("El numero de comprobante es obligatorio");
        }
        if (this.estado == EstadoDeFactura.EMITIDA) {
            throw new FacturaInmutableException("La factura ya fue emitida y es inmutable");
        }
        if (this.estado == EstadoDeFactura.ANULADA) {
            throw new FacturaInmutableException("No se puede emitir una factura anulada");
        }
        if (this.lineas.size() != 1 || this.lineas.getFirst().concepto() != ConceptoFacturable.FALSO_FLETE) {
            throw new DominioFacturacionException(
                "La emision de falso flete exige exactamente una linea con concepto FALSO_FLETE"
            );
        }

        validarConsistenciaDeImportes();

        this.falsoFlete = true;
        this.numeroDeComprobante = numeroDeComprobante;
        this.fechaDeEmision = fechaDeEmision;
        this.estado = EstadoDeFactura.EMITIDA;
    }

    private void validarConsistenciaDeImportes() {
        Dinero tot = total();
        Dinero detMonto = this.detraccion.monto();
        if (detMonto.esMayorQue(tot)) {
            throw new ImportesInconsistentesException(
                "El monto de detraccion (" + detMonto.monto() + ") supera el total (" + tot.monto() + ")"
            );
        }
        Dinero detEsperada = tot.porcentaje(this.detraccion.porcentaje());
        if (detMonto.monto().compareTo(detEsperada.monto()) != 0) {
            throw new ImportesInconsistentesException(
                "El monto de detraccion (" + detMonto.monto() + ") no coincide con el porcentaje ("
                + this.detraccion.porcentaje() + "%) del total (" + tot.monto() + "). Se esperaba: " + detEsperada.monto()
            );
        }
        Dinero neto = montoNeto();
        if (neto.sumar(detMonto).monto().compareTo(tot.monto()) != 0) {
            throw new ImportesInconsistentesException(
                "Monto neto (" + neto.monto() + ") + detraccion (" + detMonto.monto() + ") no iguala el total (" + tot.monto() + ")"
            );
        }
    }

    public void anular(OffsetDateTime fechaDeAnulacion) {
        if (fechaDeAnulacion == null) {
            throw new IllegalArgumentException("La fecha de anulacion es obligatoria");
        }
        if (this.estado != EstadoDeFactura.EMITIDA) {
            throw new DominioFacturacionException(
                "Solo se puede anular una factura en estado EMITIDA. Estado actual: " + this.estado
            );
        }
        this.estado = EstadoDeFactura.ANULADA;
    }

    public boolean correspondeA(String ordenDeServicioId) {
        if (ordenDeServicioId == null || ordenDeServicioId.isBlank()) {
            return false;
        }
        return this.ordenDeServicioId.equals(ordenDeServicioId.trim());
    }

    public String id() {
        return id;
    }

    public String ordenDeServicioId() {
        return ordenDeServicioId;
    }

    public String clienteId() {
        return clienteId;
    }

    public NumeroDeComprobante numeroDeComprobante() {
        return numeroDeComprobante;
    }

    public SnapshotComercial snapshotComercial() {
        return snapshotComercial;
    }

    public Detraccion detraccion() {
        return detraccion;
    }

    public Conformidad conformidad() {
        return conformidad;
    }

    public EstadoDeFactura estado() {
        return estado;
    }

    public List<LineaDeFactura> lineas() {
        return List.copyOf(lineas);
    }

    public List<NotaDeCredito> notasDeCredito() {
        return List.copyOf(notasDeCredito);
    }

    public OffsetDateTime fechaDeEmision() {
        return fechaDeEmision;
    }

    public boolean esFalsoFlete() {
        return falsoFlete;
    }

    public boolean falsoFlete() {
        return falsoFlete;
    }
}
