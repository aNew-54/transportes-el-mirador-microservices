package pe.edu.unc.elmirador.cobranza.models.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import pe.edu.unc.elmirador.cobranza.exceptions.DominioCobranzaException;
import pe.edu.unc.elmirador.cobranza.exceptions.RehabilitacionInvalidaException;
import pe.edu.unc.elmirador.cobranza.models.vo.Dinero;
import pe.edu.unc.elmirador.cobranza.models.vo.EstadoCrediticio;

/**
 * Raiz del agregado CuentaCorrienteDelCliente.
 * La identidad es el cliente (clienteId).
 */
@Entity
@Table(name = "cuentas_corrientes")
public class CuentaCorrienteDelCliente {

    @Id
    @Column(name = "cliente_id", length = 40, nullable = false)
    private String clienteId;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "situacion", column = @Column(name = "situacion", length = 20, nullable = false)),
        @AttributeOverride(name = "motivo", column = @Column(name = "motivo", length = 300)),
        @AttributeOverride(name = "fechaDeCambio", column = @Column(name = "fecha_de_cambio", nullable = false))
    })
    private EstadoCrediticio estado;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id", nullable = false)
    private List<CuentaPorCobrar> cuentas = new ArrayList<>();

    /** Exigido por JPA. No usar: no valida ninguna invariante. */
    protected CuentaCorrienteDelCliente() {
    }

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
        if (cuentasIniciales != null) {
            for (CuentaPorCobrar cuenta : cuentasIniciales) {
                registrarCuenta(cuenta);
            }
        }
    }

    public String clienteId() {
        return clienteId;
    }

    public String getClienteId() {
        return clienteId;
    }

    public EstadoCrediticio estado() {
        return estado;
    }

    public EstadoCrediticio getEstado() {
        return estado;
    }

    public List<CuentaPorCobrar> cuentas() {
        return List.copyOf(cuentas);
    }

    public List<CuentaPorCobrar> getCuentas() {
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

    /**
     * Deuda del cliente en la moneda indicada. Alimenta el contrato 11.
     *
     * <p>La moneda es un parametro y no se deduce de la primera cuenta: un cliente sin cuentas debe
     * responder cero, no reventar, y tomar la moneda de {@code cuentas.get(0)} seria un valor por
     * defecto silencioso en un importe.
     */
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CuentaCorrienteDelCliente that = (CuentaCorrienteDelCliente) o;
        return Objects.equals(clienteId, that.clienteId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clienteId);
    }
}
