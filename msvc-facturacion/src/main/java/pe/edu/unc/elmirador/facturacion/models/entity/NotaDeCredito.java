package pe.edu.unc.elmirador.facturacion.models.entity;

import java.time.OffsetDateTime;
import pe.edu.unc.elmirador.facturacion.exceptions.MonedaIncompatibleException;
import pe.edu.unc.elmirador.facturacion.exceptions.MontoExcedeElSaldoException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import pe.edu.unc.elmirador.facturacion.models.vo.Dinero;
import pe.edu.unc.elmirador.facturacion.models.vo.MotivoDeAjuste;

/**
 * Raiz del agregado NotaDeCredito.
 * Unico mecanismo de correccion de una factura emitida.
 */
@Entity
@Table(name = "notas_de_credito")
public class NotaDeCredito {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    // Referencia a otra raiz de agregado del MISMO contexto. Es un escalar y no lleva FK: dos
    // agregados no se acoplan con una clave foranea.
    @Column(name = "factura_id", length = 40, nullable = false)
    private String facturaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo", length = 30, nullable = false)
    private MotivoDeAjuste motivo;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "monto", column = @Column(name = "monto", precision = 15, scale = 2, nullable = false)),
        @AttributeOverride(name = "codigoMoneda", column = @Column(name = "codigo_moneda", length = 3, nullable = false))
    })
    private Dinero monto;

    @Column(name = "fecha_de_emision", nullable = false)
    private OffsetDateTime fechaDeEmision;

    @Column(name = "motivo_detalle", length = 300)
    private String motivoDetalle;

    /** Exigido por JPA. No usar: no valida ninguna invariante. */
    protected NotaDeCredito() {
    }

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
