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
import java.time.LocalDate;
import pe.edu.unc.elmirador.comercial.exceptions.CotizacionVencidaException;
import pe.edu.unc.elmirador.comercial.exceptions.DominioComercialException;
import pe.edu.unc.elmirador.comercial.models.vo.Carga;
import pe.edu.unc.elmirador.comercial.models.vo.EstadoDeCotizacion;
import pe.edu.unc.elmirador.comercial.models.vo.MotivoDeRechazo;
import pe.edu.unc.elmirador.comercial.models.vo.PeriodoDeVigencia;
import pe.edu.unc.elmirador.comercial.models.vo.Ruta;
import pe.edu.unc.elmirador.comercial.models.vo.Tarifa;

/**
 * Raiz del agregado Cotizacion.
 * Sostiene la invariante COT-01 y valida vigencia de 7 dias calendario en emitir.
 */
@Entity
@Table(name = "cotizaciones")
public class Cotizacion {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "cliente_id", length = 40, nullable = false)
    private String clienteId;

    @Column(name = "tarifario_id", length = 40, nullable = false)
    private String tarifarioId;

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
        @AttributeOverride(name = "desde", column = @Column(name = "vigencia_desde", nullable = false)),
        @AttributeOverride(name = "hasta", column = @Column(name = "vigencia_hasta", nullable = false))
    })
    private PeriodoDeVigencia vigencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoDeCotizacion estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_de_rechazo", length = 20)
    private MotivoDeRechazo motivoDeRechazo;

    /** Exigido por JPA. No usar: no valida ninguna invariante. */
    protected Cotizacion() {
    }

    public Cotizacion(
        String id,
        String clienteId,
        String tarifarioId,
        Carga carga,
        Ruta ruta,
        Tarifa tarifa,
        PeriodoDeVigencia vigencia,
        EstadoDeCotizacion estado,
        MotivoDeRechazo motivoDeRechazo
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id de cotizacion es obligatorio");
        }
        if (clienteId == null || clienteId.isBlank()) {
            throw new IllegalArgumentException("El clienteId es obligatorio");
        }
        if (tarifarioId == null || tarifarioId.isBlank()) {
            throw new IllegalArgumentException("El tarifarioId es obligatorio");
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
        if (vigencia == null) {
            throw new IllegalArgumentException("El periodo de vigencia es obligatorio");
        }
        if (estado == null) {
            throw new IllegalArgumentException("El estado de cotizacion es obligatorio");
        }
        if (estado == EstadoDeCotizacion.RECHAZADA && motivoDeRechazo == null) {
            throw new IllegalArgumentException("Una cotizacion rechazada exige motivo de rechazo");
        }
        this.id = id.trim();
        this.clienteId = clienteId.trim();
        this.tarifarioId = tarifarioId.trim();
        this.carga = carga;
        this.ruta = ruta;
        this.tarifa = tarifa;
        this.vigencia = vigencia;
        this.estado = estado;
        this.motivoDeRechazo = motivoDeRechazo;
    }

    public static Cotizacion emitir(
        String id,
        String clienteId,
        String tarifarioId,
        Carga carga,
        Ruta ruta,
        Tarifa tarifa,
        PeriodoDeVigencia vigencia
    ) {
        if (vigencia == null) {
            throw new IllegalArgumentException("El periodo de vigencia es obligatorio");
        }
        if (vigencia.diasDeVigencia() != 7) {
            throw new IllegalArgumentException(
                "La cotizacion exige una vigencia de exactamente 7 dias calendario desde la emision: "
                    + vigencia.diasDeVigencia()
            );
        }
        return new Cotizacion(
            id,
            clienteId,
            tarifarioId,
            carga,
            ruta,
            tarifa,
            vigencia,
            EstadoDeCotizacion.EMITIDA,
            null
        );
    }

    public String id() {
        return id;
    }

    public String clienteId() {
        return clienteId;
    }

    public String tarifarioId() {
        return tarifarioId;
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

    public PeriodoDeVigencia vigencia() {
        return vigencia;
    }

    public EstadoDeCotizacion estado() {
        return estado;
    }

    public MotivoDeRechazo motivoDeRechazo() {
        return motivoDeRechazo;
    }

    public boolean haVencidoEn(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }
        return !this.vigencia.estaVigenteEn(fecha);
    }

    /**
     * Invariante COT-01:
     * Una cotizacion vencida no puede aceptarse, solo recotizarse.
     * Aceptar una ya ACEPTADA o RECHAZADA tambien lanza excepcion.
     */
    public void aceptar(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }
        if (this.estado != EstadoDeCotizacion.EMITIDA) {
            throw new DominioComercialException(
                "Solo se puede aceptar una cotizacion en estado EMITIDA, estado actual: " + this.estado
            );
        }
        if (haVencidoEn(fecha)) {
            this.estado = EstadoDeCotizacion.VENCIDA;
            throw new CotizacionVencidaException(
                "La cotizacion " + this.id + " ha vencido al " + fecha + " y no puede ser aceptada"
            );
        }
        this.estado = EstadoDeCotizacion.ACEPTADA;
    }

    public void rechazar(MotivoDeRechazo motivo, LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }
        if (motivo == null) {
            throw new IllegalArgumentException("El motivo de rechazo es obligatorio");
        }
        if (this.estado != EstadoDeCotizacion.EMITIDA) {
            throw new DominioComercialException(
                "Solo se puede rechazar una cotizacion en estado EMITIDA, estado actual: " + this.estado
            );
        }
        if (haVencidoEn(fecha)) {
            this.estado = EstadoDeCotizacion.VENCIDA;
            throw new CotizacionVencidaException(
                "La cotizacion " + this.id + " ha vencido al " + fecha + " y no puede ser rechazada"
            );
        }
        this.estado = EstadoDeCotizacion.RECHAZADA;
        this.motivoDeRechazo = motivo;
    }
}
