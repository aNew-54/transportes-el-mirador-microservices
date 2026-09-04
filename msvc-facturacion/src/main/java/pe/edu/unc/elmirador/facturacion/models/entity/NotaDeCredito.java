package pe.edu.unc.elmirador.facturacion.models.entity;

import java.time.OffsetDateTime;
import pe.edu.unc.elmirador.facturacion.exceptions.MonedaIncompatibleException;
import pe.edu.unc.elmirador.facturacion.exceptions.MontoExcedeElSaldoException;
import pe.edu.unc.elmirador.facturacion.models.vo.Dinero;
import pe.edu.unc.elmirador.facturacion.models.vo.MotivoDeAjuste;

/**
 * Raiz del agregado NotaDeCredito.
 * Unico mecanismo de correccion de una factura emitida.
 */
public class NotaDeCredito {

    private final String id;
    private final String facturaId;
    private final MotivoDeAjuste motivo;
    private final Dinero monto;
    private final OffsetDateTime fechaDeEmision;
    private final String motivoDetalle;

    public NotaDeCredito(
        String id,
        String facturaId,
        MotivoDeAjuste motivo,
        String motivoDetalle,
        Dinero monto,
        OffsetDateTime fechaDeEmision
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id de la nota de credito es obligatorio");
        }
        if (facturaId == null || facturaId.isBlank()) {
            throw new IllegalArgumentException("El facturaId es obligatorio");
        }
        if (motivo == null) {
            throw new IllegalArgumentException("El motivo de ajuste es obligatorio");
        }
        if (monto == null) {
            throw new IllegalArgumentException("El monto es obligatorio");
        }
        if (monto.esCero()) {
            throw new IllegalArgumentException("El monto de la nota de credito debe ser mayor a cero");
        }
        if (fechaDeEmision == null) {
            throw new IllegalArgumentException("La fecha de emision es obligatoria");
        }

        this.id = id.trim();
        this.facturaId = facturaId.trim();
        this.motivo = motivo;
        this.motivoDetalle = motivoDetalle != null ? motivoDetalle.trim() : "";
        this.monto = monto;
        this.fechaDeEmision = fechaDeEmision;
    }

    public static NotaDeCredito emitir(
        String id,
        String facturaId,
        MotivoDeAjuste motivo,
        String motivoDetalle,
        Dinero monto,
        Dinero saldoAjustableDeLaFactura,
        OffsetDateTime fechaDeEmision
    ) {
        if (saldoAjustableDeLaFactura == null) {
            throw new IllegalArgumentException("El saldo ajustable de la factura es obligatorio");
        }
        if (monto == null) {
            throw new IllegalArgumentException("El monto es obligatorio");
        }
        if (!monto.codigoMoneda().equalsIgnoreCase(saldoAjustableDeLaFactura.codigoMoneda())) {
            throw new MonedaIncompatibleException(
                "La moneda de la nota de credito (" + monto.codigoMoneda()
                + ") no coincide con la de la factura (" + saldoAjustableDeLaFactura.codigoMoneda() + ")"
            );
        }
        if (monto.esMayorQue(saldoAjustableDeLaFactura)) {
            throw new MontoExcedeElSaldoException(
                "El monto de la nota de credito (" + monto.monto()
                + ") excede el saldo ajustable de la factura (" + saldoAjustableDeLaFactura.monto() + ")"
            );
        }

        return new NotaDeCredito(id, facturaId, motivo, motivoDetalle, monto, fechaDeEmision);
    }

    public static NotaDeCredito emitir(
        String id,
        String facturaId,
        MotivoDeAjuste motivo,
        Dinero monto,
        Dinero saldoAjustableDeLaFactura,
        OffsetDateTime fechaDeEmision
    ) {
        return emitir(id, facturaId, motivo, "", monto, saldoAjustableDeLaFactura, fechaDeEmision);
    }

    public static NotaDeCredito emitir(
        String facturaId,
        MotivoDeAjuste motivo,
        Dinero monto,
        Dinero saldoAjustableDeLaFactura,
        OffsetDateTime fechaDeEmision
    ) {
        String idGenerado = (facturaId != null ? facturaId : "NC") + "-NC";
        return emitir(idGenerado, facturaId, motivo, "", monto, saldoAjustableDeLaFactura, fechaDeEmision);
    }

    public String id() {
        return id;
    }

    public String facturaId() {
        return facturaId;
    }

    public MotivoDeAjuste motivo() {
        return motivo;
    }

    public Dinero monto() {
        return monto;
    }

    public OffsetDateTime fechaDeEmision() {
        return fechaDeEmision;
    }

    public String motivoDetalle() {
        return motivoDetalle;
    }
}
