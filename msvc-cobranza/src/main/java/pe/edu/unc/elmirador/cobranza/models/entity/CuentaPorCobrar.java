package pe.edu.unc.elmirador.cobranza.models.entity;

import java.time.LocalDate;
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
public class CuentaPorCobrar {

    private final String id;
    private final String clienteId;
    private final String facturaId;
    private final String documentoId;
    private final Dinero total;
    private final Dinero detraccion;
    private final LocalDate fechaDeVencimiento;
    private Dinero aplicado;
    private boolean detraccionDepositada;

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

    public String clienteId() {
        return clienteId;
    }

    public String facturaId() {
        return facturaId;
    }

    public String documentoId() {
        return documentoId;
    }

    public Dinero total() {
        return total;
    }

    public Dinero detraccion() {
        return detraccion;
    }

    public LocalDate fechaDeVencimiento() {
        return fechaDeVencimiento;
    }

    public Dinero aplicado() {
        return aplicado;
    }

    public boolean detraccionDepositada() {
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
}
