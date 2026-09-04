package pe.edu.unc.elmirador.cobranza.models.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import pe.edu.unc.elmirador.cobranza.exceptions.AplicacionExcedeElPagoException;
import pe.edu.unc.elmirador.cobranza.exceptions.MonedaIncompatibleException;
import pe.edu.unc.elmirador.cobranza.exceptions.PagoDeOtroClienteException;
import pe.edu.unc.elmirador.cobranza.exceptions.SaldoInsuficienteException;
import pe.edu.unc.elmirador.cobranza.models.vo.Dinero;
import pe.edu.unc.elmirador.cobranza.models.vo.MedioDePago;

/**
 * Raiz del agregado Pago.
 * Administra el registro del pago y sus aplicaciones a una o varias cuentas por cobrar.
 */
public class Pago {

    private final String id;
    private final String clienteId;
    private final Dinero monto;
    private final MedioDePago medioDePago;
    private final LocalDate fecha;
    private final List<AplicacionDePago> aplicaciones;

    public Pago(String id, String clienteId, Dinero monto, MedioDePago medioDePago, LocalDate fecha) {
        this(id, clienteId, monto, medioDePago, fecha, new ArrayList<>());
    }

    public Pago(
        String id,
        String clienteId,
        Dinero monto,
        MedioDePago medioDePago,
        LocalDate fecha,
        List<AplicacionDePago> aplicacionesIniciales
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id del pago es obligatorio");
        }
        if (clienteId == null || clienteId.isBlank()) {
            throw new IllegalArgumentException("El clienteId es obligatorio");
        }
        if (monto == null) {
            throw new IllegalArgumentException("El monto del pago es obligatorio");
        }
        if (monto.esCero()) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor a cero");
        }
        if (medioDePago == null) {
            throw new IllegalArgumentException("El medio de pago es obligatorio");
        }
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha del pago es obligatoria");
        }

        this.id = id.trim();
        this.clienteId = clienteId.trim();
        this.monto = monto;
        this.medioDePago = medioDePago;
        this.fecha = fecha;
        this.aplicaciones = new ArrayList<>();

        if (aplicacionesIniciales != null) {
            Dinero acumulado = Dinero.cero(monto.codigoMoneda());
            for (AplicacionDePago app : aplicacionesIniciales) {
                acumulado = acumulado.sumar(app.importe());
                this.aplicaciones.add(app);
            }
            if (acumulado.esMayorQue(monto)) {
                throw new AplicacionExcedeElPagoException(
                    "Las aplicaciones iniciales (" + acumulado.monto() + ") exceden el monto del pago (" + monto.monto() + ")"
                );
            }
        }
    }

    public String id() {
        return id;
    }

    public String clienteId() {
        return clienteId;
    }

    public Dinero monto() {
        return monto;
    }

    public MedioDePago medioDePago() {
        return medioDePago;
    }

    public LocalDate fecha() {
        return fecha;
    }

    public List<AplicacionDePago> aplicaciones() {
        return List.copyOf(aplicaciones);
    }

    public Dinero montoAplicado() {
        Dinero total = Dinero.cero(this.monto.codigoMoneda());
        for (AplicacionDePago app : this.aplicaciones) {
            total = total.sumar(app.importe());
        }
        return total;
    }

    public Dinero saldoSinAplicar() {
        return this.monto.restar(montoAplicado());
    }

    public void aplicarACuentaPorCobrar(CuentaPorCobrar cuenta, Dinero importe) {
        if (cuenta == null) {
            throw new IllegalArgumentException("La cuenta por cobrar es obligatoria");
        }
        if (importe == null) {
            throw new IllegalArgumentException("El importe a aplicar es obligatorio");
        }
        if (importe.esCero()) {
            throw new IllegalArgumentException("El importe a aplicar debe ser mayor a cero");
        }

        // 1. Validar PAG-02: cuenta de otro cliente
        if (!this.clienteId.equals(cuenta.clienteId())) {
            throw new PagoDeOtroClienteException(
                "El pago pertenece al cliente " + this.clienteId + " pero la cuenta pertenece al cliente " + cuenta.clienteId()
            );
        }

        // Validar compatibilidad de monedas
        if (!this.monto.codigoMoneda().equalsIgnoreCase(importe.codigoMoneda())) {
            throw new MonedaIncompatibleException(
                "La moneda del importe (" + importe.codigoMoneda() + ") no coincide con la del pago (" + this.monto.codigoMoneda() + ")"
            );
        }
        if (!cuenta.total().codigoMoneda().equalsIgnoreCase(importe.codigoMoneda())) {
            throw new MonedaIncompatibleException(
                "La moneda del importe (" + importe.codigoMoneda() + ") no coincide con la de la cuenta (" + cuenta.total().codigoMoneda() + ")"
            );
        }

        // 2. Validar PAG-01: suma de aplicaciones no excede el monto del pago
        Dinero nuevoMontoAplicado = montoAplicado().sumar(importe);
        if (nuevoMontoAplicado.esMayorQue(this.monto)) {
            throw new AplicacionExcedeElPagoException(
                "La aplicacion de " + importe.monto() + " excede el saldo sin aplicar del pago (" + saldoSinAplicar().monto() + ")"
            );
        }

        // 3. Validar CCC-02: no exceder saldo de la cuenta antes de mutar el pago
        if (importe.esMayorQue(cuenta.saldo())) {
            throw new SaldoInsuficienteException(
                "El importe a aplicar (" + importe.monto() + ") excede el saldo pendiente de la cuenta (" + cuenta.saldo().monto() + ")"
            );
        }

        // Todo valido: mutamos cuenta y registramos aplicacion
        cuenta.aplicar(importe);
        String aplicacionId = this.id + "-APP-" + (this.aplicaciones.size() + 1);
        this.aplicaciones.add(new AplicacionDePago(aplicacionId, cuenta.id(), importe));
    }
}
