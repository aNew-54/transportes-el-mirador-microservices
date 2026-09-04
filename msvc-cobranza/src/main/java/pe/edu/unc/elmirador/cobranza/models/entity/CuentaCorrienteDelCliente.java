package pe.edu.unc.elmirador.cobranza.models.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import pe.edu.unc.elmirador.cobranza.exceptions.DominioCobranzaException;
import pe.edu.unc.elmirador.cobranza.exceptions.RehabilitacionInvalidaException;
import pe.edu.unc.elmirador.cobranza.models.vo.Dinero;
import pe.edu.unc.elmirador.cobranza.models.vo.EstadoCrediticio;

/**
 * Raiz del agregado CuentaCorrienteDelCliente.
 * La identidad es el cliente (clienteId).
 */
public class CuentaCorrienteDelCliente {

    private final String clienteId;
    private EstadoCrediticio estado;
    private final List<CuentaPorCobrar> cuentas;

    public CuentaCorrienteDelCliente(String clienteId, EstadoCrediticio estado) {
        this(clienteId, estado, new ArrayList<>());
    }

    public CuentaCorrienteDelCliente(String clienteId, EstadoCrediticio estado, List<CuentaPorCobrar> cuentasIniciales) {
        if (clienteId == null || clienteId.isBlank()) {
            throw new IllegalArgumentException("El clienteId es obligatorio");
        }
        if (estado == null) {
            throw new IllegalArgumentException("El estado crediticio es obligatorio");
        }
        this.clienteId = clienteId.trim();
        this.estado = estado;
        this.cuentas = new ArrayList<>();
        if (cuentasIniciales != null) {
            for (CuentaPorCobrar cuenta : cuentasIniciales) {
                registrarCuenta(cuenta);
            }
        }
    }

    public String clienteId() {
        return clienteId;
    }

    public EstadoCrediticio estado() {
        return estado;
    }

    public List<CuentaPorCobrar> cuentas() {
        return List.copyOf(cuentas);
    }

    public void registrarCuenta(CuentaPorCobrar cuenta) {
        if (cuenta == null) {
            throw new IllegalArgumentException("La cuenta por cobrar es obligatoria");
        }
        if (!this.clienteId.equals(cuenta.clienteId())) {
            throw new DominioCobranzaException(
                "La cuenta por cobrar pertenece al cliente " + cuenta.clienteId() + " pero la cuenta corriente es de " + this.clienteId
            );
        }
        boolean existeFactura = this.cuentas.stream()
            .anyMatch(c -> c.facturaId().equals(cuenta.facturaId()));
        if (existeFactura) {
            throw new DominioCobranzaException(
                "La factura " + cuenta.facturaId() + " ya se encuentra registrada en la cuenta corriente"
            );
        }
        this.cuentas.add(cuenta);
    }

    public void evaluarCredito(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }
        boolean tieneMoraSuperiorATreintaDias = this.cuentas.stream()
            .filter(c -> !c.estaCancelada())
            .anyMatch(c -> c.diasDeAtraso(fecha).superaLosTreinta());

        if (tieneMoraSuperiorATreintaDias) {
            this.estado = EstadoCrediticio.suspendido(
                "Suspension automatica por mora superior a 30 dias",
                fecha
            );
        }
    }

    public void suspenderCredito(String motivo, LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo de suspension es obligatorio");
        }
        this.estado = EstadoCrediticio.suspendido(motivo, fecha);
    }

    public void rehabilitarCredito(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }
        boolean tieneMoraSuperiorATreintaDias = this.cuentas.stream()
            .filter(c -> !c.estaCancelada())
            .anyMatch(c -> c.diasDeAtraso(fecha).superaLosTreinta());

        if (tieneMoraSuperiorATreintaDias) {
            throw new RehabilitacionInvalidaException(
                "No se puede rehabilitar el credito: existen cuentas con mas de 30 dias de atraso"
            );
        }
        this.estado = EstadoCrediticio.vigente(fecha);
    }

    public Dinero deudaTotal() {
        if (this.cuentas.isEmpty()) {
            throw new IllegalStateException("No hay cuentas registradas para determinar la moneda de la deuda total");
        }
        return deudaTotal(this.cuentas.get(0).total().codigoMoneda());
    }

    public Dinero deudaTotal(String codigoMoneda) {
        if (codigoMoneda == null || codigoMoneda.isBlank()) {
            throw new IllegalArgumentException("El codigo de moneda es obligatorio");
        }
        Dinero total = Dinero.cero(codigoMoneda);
        for (CuentaPorCobrar cuenta : this.cuentas) {
            if (!cuenta.estaCancelada()) {
                total = total.sumar(cuenta.saldo());
            }
        }
        return total;
    }

    public int diasDeAtrasoMaximo(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }
        return this.cuentas.stream()
            .filter(c -> !c.estaCancelada())
            .mapToInt(c -> Math.max(0, c.diasDeAtraso(fecha).dias()))
            .max()
            .orElse(0);
    }

    public int cuentasVencidas(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }
        return (int) this.cuentas.stream()
            .filter(c -> !c.estaCancelada() && c.diasDeAtraso(fecha).dias() > 0)
            .count();
    }
}
