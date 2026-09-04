package pe.edu.unc.elmirador.ejecucion.models.entity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import pe.edu.unc.elmirador.ejecucion.exceptions.GastoSinComprobanteException;
import pe.edu.unc.elmirador.ejecucion.exceptions.LiquidacionAprobadaException;
import pe.edu.unc.elmirador.ejecucion.models.vo.Dinero;
import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoDeLiquidacion;
import pe.edu.unc.elmirador.ejecucion.models.vo.Saldo;

/**
 * Raiz de agregado LiquidacionDeViaje con identidad compuesta (viajeId + conductorId).
 * En viajes con relevo coexisten dos liquidaciones independientes.
 * El saldo se calcula dinamicamente y NUNCA se almacena (LIQ-02, D8).
 */
public class LiquidacionDeViaje {

    private final String viajeId;
    private final String conductorId;
    private final Dinero anticipo;
    private final List<GastoDeRuta> gastos;
    private EstadoDeLiquidacion estado;
    private OffsetDateTime fechaDeAprobacion;
    private String motivoObservacion;

    public static LiquidacionDeViaje abrir(String viajeId, String conductorId, Dinero anticipo) {
        return new LiquidacionDeViaje(viajeId, conductorId, anticipo);
    }

    public LiquidacionDeViaje(String viajeId, String conductorId, Dinero anticipo) {
        if (viajeId == null || viajeId.isBlank()) {
            throw new IllegalArgumentException("El viajeId es obligatorio");
        }
        if (conductorId == null || conductorId.isBlank()) {
            throw new IllegalArgumentException("El conductorId es obligatorio");
        }
        if (anticipo == null) {
            throw new IllegalArgumentException("El anticipo es obligatorio");
        }
        this.viajeId = viajeId.trim();
        this.conductorId = conductorId.trim();
        this.anticipo = anticipo;
        this.gastos = new ArrayList<>();
        this.estado = EstadoDeLiquidacion.ABIERTA;
        this.fechaDeAprobacion = null;
        this.motivoObservacion = null;
    }

    public void rendirGasto(GastoDeRuta gasto) {
        if (this.estado == EstadoDeLiquidacion.APROBADA) {
            throw new LiquidacionAprobadaException(
                "No se pueden rendir gastos sobre una liquidacion aprobada (LIQ-03)"
            );
        }
        if (gasto == null) {
            throw new IllegalArgumentException("El gasto es obligatorio");
        }
        if (gasto.getComprobante() == null) {
            throw new GastoSinComprobanteException("Todo gasto rendido debe contar con comprobante (LIQ-01)");
        }
        this.anticipo.sumar(gasto.getImporte());
        this.gastos.add(gasto);
    }

    public Dinero totalDeGastos() {
        Dinero total = Dinero.cero(anticipo.codigoMoneda());
        for (GastoDeRuta g : gastos) {
            total = total.sumar(g.getImporte());
        }
        return total;
    }

    public Saldo saldo() {
        return Saldo.entre(anticipo, totalDeGastos());
    }

    public void aprobar(OffsetDateTime fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de aprobacion es obligatoria");
        }
        if (this.estado == EstadoDeLiquidacion.APROBADA) {
            throw new LiquidacionAprobadaException("La liquidacion ya esta aprobada (LIQ-03)");
        }
        this.estado = EstadoDeLiquidacion.APROBADA;
        this.fechaDeAprobacion = fecha;
    }

    public void observar(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo de observacion es obligatorio");
        }
        if (this.estado == EstadoDeLiquidacion.APROBADA) {
            throw new LiquidacionAprobadaException("No se puede observar una liquidacion aprobada (LIQ-03)");
        }
        this.estado = EstadoDeLiquidacion.OBSERVADA;
        this.motivoObservacion = motivo.trim();
    }

    public boolean estaPendiente() {
        return this.estado != EstadoDeLiquidacion.APROBADA;
    }

    public String getViajeId() {
        return viajeId;
    }

    public String getConductorId() {
        return conductorId;
    }

    public Dinero getAnticipo() {
        return anticipo;
    }

    public List<GastoDeRuta> getGastos() {
        return List.copyOf(gastos);
    }

    public EstadoDeLiquidacion getEstado() {
        return estado;
    }

    public OffsetDateTime getFechaDeAprobacion() {
        return fechaDeAprobacion;
    }

    public String getMotivoObservacion() {
        return motivoObservacion;
    }
}
