package pe.edu.unc.elmirador.comercial.models.entity;

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
public class Cotizacion {

    private final String id;
    private final String clienteId;
    private final String tarifarioId;
    private final Carga carga;
    private final Ruta ruta;
    private final Tarifa tarifa;
    private final PeriodoDeVigencia vigencia;
    private EstadoDeCotizacion estado;
    private MotivoDeRechazo motivoDeRechazo;

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
