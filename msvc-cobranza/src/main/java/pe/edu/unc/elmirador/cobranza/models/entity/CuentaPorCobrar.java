package pe.edu.unc.elmirador.cobranza.models.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;
import pe.edu.unc.elmirador.cobranza.exceptions.DominioCobranzaException;
import pe.edu.unc.elmirador.cobranza.exceptions.ImportesInconsistentesException;
import pe.edu.unc.elmirador.cobranza.exceptions.MonedaIncompatibleException;
import pe.edu.unc.elmirador.cobranza.exceptions.SaldoInsuficienteException;
import pe.edu.unc.elmirador.cobranza.models.vo.DiasDeAtraso;
import pe.edu.unc.elmirador.cobranza.models.vo.Dinero;
import pe.edu.unc.elmirador.cobranza.models.vo.EstadoDeDocumento;

/**
 * Entidad hija de CuentaCorrienteDelCliente.
 * Representa una cuenta por cobrar originada por una factura a credito.
 */
@Entity
@Table(name = "cuentas_por_cobrar")
public class CuentaPorCobrar {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "cliente_id", length = 40, nullable = false, insertable = false, updatable = false)
    private String clienteId;

    @Column(name = "factura_id", length = 40, nullable = false)
    private String facturaId;

    @Column(name = "documento_id", length = 40, nullable = false)
    private String documentoId;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "monto", column = @Column(name = "total_monto", precision = 15, scale = 2, nullable = false)),
        @AttributeOverride(name = "codigoMoneda", column = @Column(name = "total_moneda", length = 3, nullable = false))
    })
    private Dinero total;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "monto", column = @Column(name = "detraccion_monto", precision = 15, scale = 2, nullable = false)),
        @AttributeOverride(name = "codigoMoneda", column = @Column(name = "detraccion_moneda", length = 3, nullable = false))
    })
    private Dinero detraccion;

    @Column(name = "fecha_de_vencimiento", nullable = false)
    private LocalDate fechaDeVencimiento;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "monto", column = @Column(name = "aplicado_monto", precision = 15, scale = 2, nullable = false)),
        @AttributeOverride(name = "codigoMoneda", column = @Column(name = "aplicado_moneda", length = 3, nullable = false))
    })
    private Dinero aplicado;

    @Column(name = "detraccion_depositada", nullable = false)
    private boolean detraccionDepositada;

    /** Exigido por JPA. No usar: no valida ninguna invariante. */
    protected CuentaPorCobrar() {
    }

    public CuentaPorCobrar(
        String id,
        String clienteId,
        String facturaId,
        String documentoId,
        Dinero total,
        Dinero detraccion,
        Dinero montoNeto,
        LocalDate fechaDeVencimiento,
        Dinero aplicado,
        boolean detraccionDepositada
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id es obligatorio");
        }
        if (clienteId == null || clienteId.isBlank()) {
            throw new IllegalArgumentException("El clienteId es obligatorio");
        }
        if (facturaId == null || facturaId.isBlank()) {
            throw new IllegalArgumentException("El facturaId es obligatorio");
        }
        if (documentoId == null || documentoId.isBlank()) {
            throw new IllegalArgumentException("El documentoId es obligatorio");
        }
        if (total == null) {
            throw new IllegalArgumentException("El total es obligatorio");
        }
        if (detraccion == null) {
            throw new IllegalArgumentException("La detraccion es obligatoria");
        }
        if (montoNeto == null) {
            throw new IllegalArgumentException("El monto neto es obligatorio");
        }
        if (fechaDeVencimiento == null) {
            throw new IllegalArgumentException("La fecha de vencimiento es obligatoria");
        }
        if (aplicado == null) {
            throw new IllegalArgumentException("El monto aplicado es obligatorio");
        }

        if (!total.codigoMoneda().equalsIgnoreCase(detraccion.codigoMoneda())) {
            throw new MonedaIncompatibleException(
                "La moneda de la detraccion (" + detraccion.codigoMoneda() + ") no coincide con la del total (" + total.codigoMoneda() + ")"
            );
        }
        if (!total.codigoMoneda().equalsIgnoreCase(montoNeto.codigoMoneda())) {
            throw new MonedaIncompatibleException(
                "La moneda del monto neto (" + montoNeto.codigoMoneda() + ") no coincide con la del total (" + total.codigoMoneda() + ")"
            );
        }
        if (!total.codigoMoneda().equalsIgnoreCase(aplicado.codigoMoneda())) {
            throw new MonedaIncompatibleException(
                "La moneda del aplicado (" + aplicado.codigoMoneda() + ") no coincide con la del total (" + total.codigoMoneda() + ")"
            );
        }

        Dinero suma = montoNeto.sumar(detraccion);
        if (suma.monto().compareTo(total.monto()) != 0) {
            throw new ImportesInconsistentesException(
                "Monto neto (" + montoNeto.monto() + ") + detraccion (" + detraccion.monto() + ") no iguala el total (" + total.monto() + ")"
            );
        }

        if (aplicado.esMayorQue(montoNeto)) {
            throw new SaldoInsuficienteException(
                "El importe aplicado (" + aplicado.monto() + ") excede el monto neto (" + montoNeto.monto() + ")"
            );
        }

        this.id = id.trim();
        this.clienteId = clienteId.trim();
        this.facturaId = facturaId.trim();
        this.documentoId = documentoId.trim();
        this.total = total;
        this.detraccion = detraccion;
        this.fechaDeVencimiento = fechaDeVencimiento;
        this.aplicado = aplicado;
        this.detraccionDepositada = detraccionDepositada;
    }

    /**
     * Abre una cuenta con el saldo sin aplicar y la detraccion pendiente.
     *
     * <p>{@code montoNeto} se recibe, no se deduce. Es el tercer importe del contrato 10 y existe
     * justamente para contrastarlo: deducirlo como {@code total - detraccion} haria que
     * {@code montoNeto + detraccion == total} se cumpliera por construccion y FAC-04 no pudiera
     * fallar nunca por esa via. Cobranza rechaza los importes que no cuadran; no los corrige.
     */
    public CuentaPorCobrar(
        String id,
        String clienteId,
        String facturaId,
        String documentoId,
        Dinero total,
        Dinero detraccion,
        Dinero montoNeto,
        LocalDate fechaDeVencimiento
    ) {
        this(
            id,
            clienteId,
            facturaId,
            documentoId,
            total,
            detraccion,
            montoNeto,
            fechaDeVencimiento,
            total != null ? Dinero.cero(total.codigoMoneda()) : null,
            false
        );
    }

    public String id() {
        return id;
    }

    public String getId() {
        return id;
    }

    public String clienteId() {
        return clienteId;
    }

    public String getClienteId() {
        return clienteId;
    }

    public String facturaId() {
        return facturaId;
    }

    public String getFacturaId() {
        return facturaId;
    }

    public String documentoId() {
        return documentoId;
    }

    public String getDocumentoId() {
        return documentoId;
    }

    public Dinero total() {
        return total;
    }

    public Dinero getTotal() {
        return total;
    }

    public Dinero detraccion() {
        return detraccion;
    }

    public Dinero getDetraccion() {
        return detraccion;
    }

    public LocalDate fechaDeVencimiento() {
        return fechaDeVencimiento;
    }

    public LocalDate getFechaDeVencimiento() {
        return fechaDeVencimiento;
    }

    public Dinero aplicado() {
        return aplicado;
    }

    public Dinero getAplicado() {
        return aplicado;
    }

    public boolean detraccionDepositada() {
        return detraccionDepositada;
    }

    public boolean isDetraccionDepositada() {
        return detraccionDepositada;
    }

    public Dinero montoNeto() {
        return total.restar(detraccion);
    }

    public Dinero saldo() {
        return montoNeto().restar(aplicado);
    }

    public void aplicar(Dinero importe) {
        if (importe == null) {
            throw new IllegalArgumentException("El importe a aplicar es obligatorio");
        }
        if (importe.esCero()) {
            throw new IllegalArgumentException("El importe a aplicar debe ser mayor a cero");
        }
        if (!this.total.codigoMoneda().equalsIgnoreCase(importe.codigoMoneda())) {
            throw new MonedaIncompatibleException(
                "La moneda del importe (" + importe.codigoMoneda() + ") no coincide con la de la cuenta (" + this.total.codigoMoneda() + ")"
            );
        }
        Dinero saldoActual = saldo();
        if (importe.esMayorQue(saldoActual)) {
            throw new SaldoInsuficienteException(
                "El importe a aplicar (" + importe.monto() + ") excede el saldo pendiente (" + saldoActual.monto() + ")"
            );
        }
        this.aplicado = this.aplicado.sumar(importe);
    }

    public void registrarDepositoDeDetraccion() {
        if (this.detraccion.esCero()) {
            throw new DominioCobranzaException("No se puede registrar deposito de detraccion en una cuenta sin detraccion");
        }
        this.detraccionDepositada = true;
    }

    public boolean estaCancelada() {
        return this.saldo().esCero() && (this.detraccion.esCero() || this.detraccionDepositada);
    }

    public EstadoDeDocumento estadoEn(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }
        if (estaCancelada()) {
            return EstadoDeDocumento.CANCELADO;
        }
        if (fecha.isAfter(this.fechaDeVencimiento)) {
            return EstadoDeDocumento.VENCIDA;
        }
        return EstadoDeDocumento.VIGENTE;
    }

    public DiasDeAtraso diasDeAtraso(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }
        return DiasDeAtraso.entre(this.fechaDeVencimiento, fecha);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CuentaPorCobrar that = (CuentaPorCobrar) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
