package pe.edu.unc.elmirador.comercial.models.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import pe.edu.unc.elmirador.comercial.exceptions.CondicionDePagoInconsistenteException;
import pe.edu.unc.elmirador.comercial.exceptions.MonedaIncompatibleException;
import pe.edu.unc.elmirador.comercial.exceptions.ReajusteRequeridoException;
import pe.edu.unc.elmirador.comercial.exceptions.TransicionDeOrdenInvalidaException;
import pe.edu.unc.elmirador.comercial.models.vo.Carga;
import pe.edu.unc.elmirador.comercial.models.vo.CondicionDePago;
import pe.edu.unc.elmirador.comercial.models.vo.Dinero;
import pe.edu.unc.elmirador.comercial.models.vo.EstadoCrediticio;
import pe.edu.unc.elmirador.comercial.models.vo.EstadoDeOrden;
import pe.edu.unc.elmirador.comercial.models.vo.Recargo;
import pe.edu.unc.elmirador.comercial.models.vo.Ruta;
import pe.edu.unc.elmirador.comercial.models.vo.Tarifa;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeRecargo;

/**
 * Raiz del agregado OrdenDeServicio.
 * Sostiene las invariantes ORD-01 y ORD-02.
 */
@Entity
@Table(name = "ordenes_de_servicio")
public class OrdenDeServicio {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "cliente_id", length = 40, nullable = false)
    private String clienteId;

    @Column(name = "contrato_id", length = 40)
    private String contratoId;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "pesoKg", column = @Column(name = "carga_peso_kg", nullable = false)),
        @AttributeOverride(name = "volumenM3", column = @Column(name = "carga_volumen_m3", precision = 10, scale = 2, nullable = false)),
        @AttributeOverride(name = "tipo", column = @Column(name = "carga_tipo", length = 30, nullable = false))
    })
    private Carga carga;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "origen", column = @Column(name = "ruta_origen", length = 100, nullable = false)),
        @AttributeOverride(name = "destino", column = @Column(name = "ruta_destino", length = 100, nullable = false)),
        @AttributeOverride(name = "corredor", column = @Column(name = "ruta_corredor", length = 100, nullable = false))
    })
    private Ruta ruta;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "base.monto", column = @Column(name = "tarifa_base_monto", precision = 15, scale = 2, nullable = false)),
        @AttributeOverride(name = "base.codigoMoneda", column = @Column(name = "tarifa_base_moneda", length = 3, nullable = false)),
        @AttributeOverride(name = "descuento.porcentaje", column = @Column(name = "tarifa_descuento_porcentaje", precision = 5, scale = 2)),
        @AttributeOverride(name = "descuento.autorizadoPor", column = @Column(name = "tarifa_descuento_autorizado_por", length = 100)),
        @AttributeOverride(name = "recargos", column = @Column(name = "tarifa_recargos", length = 500, nullable = false))
    })
    private Tarifa tarifa;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "modalidad", column = @Column(name = "condicion_pago_modalidad", length = 10, nullable = false)),
        @AttributeOverride(name = "plazoEnDias", column = @Column(name = "condicion_pago_plazo_dias", nullable = false))
    })
    private CondicionDePago condicionDePago;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoDeOrden estado;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "base.monto", column = @Column(name = "falso_flete_base_monto", precision = 15, scale = 2)),
        @AttributeOverride(name = "base.codigoMoneda", column = @Column(name = "falso_flete_base_moneda", length = 3)),
        @AttributeOverride(name = "descuento.porcentaje", column = @Column(name = "falso_flete_descuento_porcentaje", precision = 5, scale = 2)),
        @AttributeOverride(name = "descuento.autorizadoPor", column = @Column(name = "falso_flete_descuento_autorizado_por", length = 100)),
        @AttributeOverride(name = "recargos", column = @Column(name = "falso_flete_recargos", length = 500))
    })
    private Tarifa falsoFlete;

    @Column(name = "cancelado_por", length = 100)
    private String canceladoPor;

    /** Exigido por JPA. No usar: no valida ninguna invariante. */
    protected OrdenDeServicio() {
    }

    public OrdenDeServicio(
        String id,
        String clienteId,
        String contratoId,
        Carga carga,
        Ruta ruta,
        Tarifa tarifa,
        CondicionDePago condicionDePago,
        EstadoDeOrden estado,
        Tarifa falsoFlete
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id de la orden es obligatorio");
        }
        if (clienteId == null || clienteId.isBlank()) {
            throw new IllegalArgumentException("El clienteId es obligatorio");
        }
        if (carga == null) {
            throw new IllegalArgumentException("La carga es obligatoria");
        }
        if (ruta == null) {
            throw new IllegalArgumentException("La ruta es obligatoria");
        }
        if (tarifa == null) {
            throw new IllegalArgumentException("La tarifa es obligatoria");
        }
        if (condicionDePago == null) {
            throw new IllegalArgumentException("La condicion de pago es obligatoria");
        }
        if (estado == null) {
            throw new IllegalArgumentException("El estado de la orden es obligatorio");
        }
        this.id = id.trim();
        this.clienteId = clienteId.trim();
        this.contratoId = contratoId != null && !contratoId.isBlank() ? contratoId.trim() : null;
        this.carga = carga;
        this.ruta = ruta;
        this.tarifa = tarifa;
        this.condicionDePago = condicionDePago;
        this.estado = estado;
        this.falsoFlete = falsoFlete;
    }

    /**
     * Fabrica de creacion que sostiene la invariante ORD-02:
     * Exige el EstadoCrediticio vigente y obligatorio (D2).
     * Si la condicion es CREDITO y el estado es SUSPENDIDO, lanza CondicionDePagoInconsistenteException.
     */
    public static OrdenDeServicio crear(
        String id,
        String clienteId,
        String contratoId,
        Carga carga,
        Ruta ruta,
        Tarifa tarifa,
        CondicionDePago condicionDePago,
        EstadoCrediticio estadoCrediticio
    ) {
        if (estadoCrediticio == null) {
            throw new IllegalArgumentException("El estado crediticio es obligatorio para verificar la condicion de pago");
        }
        if (condicionDePago == null) {
            throw new IllegalArgumentException("La condicion de pago es obligatoria");
        }
        if (condicionDePago.esACredito() && !estadoCrediticio.permiteCredito()) {
            throw new CondicionDePagoInconsistenteException(
                "No se puede registrar una orden a CREDITO para un cliente con situacion crediticia: "
                    + estadoCrediticio.situacion()
            );
        }
        return new OrdenDeServicio(
            id,
            clienteId,
            contratoId,
            carga,
            ruta,
            tarifa,
            condicionDePago,
            EstadoDeOrden.BORRADOR,
            null
        );
    }

    public String id() {
        return id;
    }

    public String clienteId() {
        return clienteId;
    }

    public String contratoId() {
        return contratoId;
    }

    public Carga carga() {
        return carga;
    }

    public Ruta ruta() {
        return ruta;
    }

    public Tarifa tarifa() {
        return tarifa;
    }

    public CondicionDePago condicionDePago() {
        return condicionDePago;
    }

    public EstadoDeOrden estado() {
        return estado;
    }

    public Tarifa falsoFlete() {
        return falsoFlete;
    }

    public String canceladoPor() {
        return canceladoPor;
    }

    public void confirmar() {
        if (this.estado != EstadoDeOrden.BORRADOR) {
            throw new TransicionDeOrdenInvalidaException(
                "Solo se puede confirmar una orden en estado BORRADOR, estado actual: " + this.estado
            );
        }
        this.estado = EstadoDeOrden.CONFIRMADA;
    }

    public void marcarProgramada() {
        if (this.estado != EstadoDeOrden.CONFIRMADA) {
            throw new TransicionDeOrdenInvalidaException(
                "Solo se puede programar una orden en estado CONFIRMADA, estado actual: " + this.estado
            );
        }
        this.estado = EstadoDeOrden.PROGRAMADA;
    }

    public void marcarDespachada() {
        if (this.estado != EstadoDeOrden.PROGRAMADA) {
            throw new TransicionDeOrdenInvalidaException(
                "Solo se puede marcar despachada una orden en estado PROGRAMADA, estado actual: " + this.estado
            );
        }
        this.estado = EstadoDeOrden.DESPACHADA;
    }

    /**
     * Invariante ORD-01:
     * En BORRADOR o CONFIRMADA cambia la carga sin mas.
     * En PROGRAMADA o DESPACHADA exige el importe del reajuste y lo anade como recargo.
     * Sin importe lanza ReajusteRequeridoException.
     */
    public void reajustarCarga(Carga nuevaCarga, Dinero importeDelReajuste) {
        if (nuevaCarga == null) {
            throw new IllegalArgumentException("La nueva carga es obligatoria");
        }
        if (this.estado == EstadoDeOrden.CANCELADA) {
            throw new TransicionDeOrdenInvalidaException("No se puede reajustar la carga de una orden cancelada");
        }
        if (this.estado == EstadoDeOrden.BORRADOR || this.estado == EstadoDeOrden.CONFIRMADA) {
            this.carga = nuevaCarga;
            if (importeDelReajuste != null && !importeDelReajuste.esCero()) {
                aplicarRecargoDeReajuste(importeDelReajuste);
            }
        } else if (this.estado == EstadoDeOrden.PROGRAMADA || this.estado == EstadoDeOrden.DESPACHADA) {
            if (importeDelReajuste == null || importeDelReajuste.esCero()) {
                throw new ReajusteRequeridoException(
                    "Una orden en estado " + this.estado + " no admite cambio de carga sin generar reajuste"
                );
            }
            this.carga = nuevaCarga;
            aplicarRecargoDeReajuste(importeDelReajuste);
        } else {
            throw new TransicionDeOrdenInvalidaException(
                "Estado no valido para reajuste de carga: " + this.estado
            );
        }
    }

    private void aplicarRecargoDeReajuste(Dinero importeDelReajuste) {
        if (!this.tarifa.base().codigoMoneda().equalsIgnoreCase(importeDelReajuste.codigoMoneda())) {
            throw new MonedaIncompatibleException(
                "La moneda del reajuste (" + importeDelReajuste.codigoMoneda()
                    + ") no coincide con la de la tarifa (" + this.tarifa.base().codigoMoneda() + ")"
            );
        }
        BigDecimal porcentaje = importeDelReajuste.monto()
            .multiply(BigDecimal.valueOf(100))
            .divide(this.tarifa.base().monto(), 2, RoundingMode.HALF_UP);
        Recargo recargo = new Recargo(TipoDeRecargo.REAJUSTE, porcentaje);
        List<Recargo> listaRecargos = new ArrayList<>(this.tarifa.recargos());
        listaRecargos.add(recargo);
        this.tarifa = new Tarifa(this.tarifa.base(), listaRecargos, this.tarifa.descuento());
    }

    /**
     * Cancela la orden de servicio.
     * Antes del despacho, cancela sin penalidad.
     * Despues del despacho genera falso flete por la mitad de la tarifa y exige autorizacion de gerencia registrada.
     */
    public void cancelar(LocalDate fecha, String autorizadoPor) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de cancelacion es obligatoria");
        }
        if (this.estado == EstadoDeOrden.CANCELADA) {
            throw new TransicionDeOrdenInvalidaException("La orden ya se encuentra cancelada");
        }
        if (this.estado == EstadoDeOrden.DESPACHADA) {
            if (autorizadoPor == null || autorizadoPor.isBlank()) {
                throw new IllegalArgumentException(
                    "La cancelacion posterior al despacho exige autorizacion de gerencia registrada"
                );
            }
            this.falsoFlete = new Tarifa(this.tarifa.total().mitad());
            this.canceladoPor = autorizadoPor.trim();
        } else {
            this.falsoFlete = null;
            this.canceladoPor = autorizadoPor != null && !autorizadoPor.isBlank() ? autorizadoPor.trim() : null;
        }
        this.estado = EstadoDeOrden.CANCELADA;
    }
}
