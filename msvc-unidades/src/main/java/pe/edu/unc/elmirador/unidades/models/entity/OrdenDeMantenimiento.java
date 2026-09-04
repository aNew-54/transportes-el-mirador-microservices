package pe.edu.unc.elmirador.unidades.models.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import pe.edu.unc.elmirador.unidades.exceptions.KilometrajeDeAtencionInvalidoException;
import pe.edu.unc.elmirador.unidades.exceptions.OrdenCerradaException;
import pe.edu.unc.elmirador.unidades.models.vo.Dinero;
import pe.edu.unc.elmirador.unidades.models.vo.EstadoDeOrden;
import pe.edu.unc.elmirador.unidades.models.vo.Kilometraje;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeMantenimiento;

public class OrdenDeMantenimiento {

    private final String id;
    private final String unidadId;
    private final TipoDeMantenimiento tipo;
    private final Kilometraje kmAtencion;
    private EstadoDeOrden estado;
    private final List<TrabajoRealizado> trabajos = new ArrayList<>();
    private final LocalDate fechaApertura;
    private LocalDate fechaCierre;
    private final String codigoMoneda;

    public OrdenDeMantenimiento(
            String id,
            String unidadId,
            TipoDeMantenimiento tipo,
            Kilometraje kmAtencion,
            EstadoDeOrden estado,
            List<TrabajoRealizado> trabajos,
            LocalDate fechaApertura,
            LocalDate fechaCierre,
            String codigoMoneda) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id de la orden no puede estar vacio");
        }
        if (unidadId == null || unidadId.isBlank()) {
            throw new IllegalArgumentException("El unidadId no puede estar vacio");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de mantenimiento no puede ser nulo");
        }
        if (kmAtencion == null) {
            throw new IllegalArgumentException("El kilometraje de atencion no puede ser nulo");
        }
        if (fechaApertura == null) {
            throw new IllegalArgumentException("La fecha de apertura no puede ser nula");
        }
        if (codigoMoneda == null || codigoMoneda.isBlank()) {
            throw new IllegalArgumentException(
                    "El codigo de moneda de la orden es obligatorio: no se adivina la moneda de un importe");
        }
        this.id = id;
        this.unidadId = unidadId;
        this.tipo = tipo;
        this.kmAtencion = kmAtencion;
        this.estado = (estado != null) ? estado : EstadoDeOrden.ABIERTA;
        if (trabajos != null) {
            this.trabajos.addAll(trabajos);
        }
        this.fechaApertura = fechaApertura;
        this.fechaCierre = fechaCierre;
        this.codigoMoneda = codigoMoneda.trim().toUpperCase();
    }

    /**
     * OMT-02: el kilometraje de atencion no puede ser menor al del ultimo mantenimiento de la
     * unidad. {@code kmUltimoMantenimiento} es obligatorio a proposito: aceptar null convertiria
     * la invariante en opcional, y una invariante que se puede saltar pasando null no existe.
     */
    public static OrdenDeMantenimiento abrir(
            String id,
            String unidadId,
            TipoDeMantenimiento tipo,
            Kilometraje kmAtencion,
            Kilometraje kmUltimoMantenimiento,
            LocalDate fechaApertura,
            String codigoMoneda) {
        if (kmAtencion == null) {
            throw new IllegalArgumentException("El kilometraje de atencion no puede ser nulo");
        }
        if (kmUltimoMantenimiento == null) {
            throw new IllegalArgumentException(
                    "El kilometraje del ultimo mantenimiento es obligatorio para evaluar OMT-02");
        }
        if (kmAtencion.valor() < kmUltimoMantenimiento.valor()) {
            throw new KilometrajeDeAtencionInvalidoException(
                    "El kilometraje registrado no puede ser menor al del ultimo mantenimiento (OMT-02): atencion="
                            + kmAtencion.valor() + ", ultimo=" + kmUltimoMantenimiento.valor());
        }
        return new OrdenDeMantenimiento(
                id, unidadId, tipo, kmAtencion, EstadoDeOrden.ABIERTA, null, fechaApertura, null, codigoMoneda);
    }

    public void registrarTrabajo(TrabajoRealizado trabajo) {
        if (this.estado == EstadoDeOrden.CERRADA) {
            throw new OrdenCerradaException(
                    "No se puede registrar un trabajo en una orden cerrada (OMT-01): " + id);
        }
        if (trabajo == null) {
            throw new IllegalArgumentException("El trabajo realizado no puede ser nulo");
        }
        this.trabajos.add(trabajo);
    }

    public void cerrar(LocalDate fechaCierre) {
        if (this.estado == EstadoDeOrden.CERRADA) {
            throw new OrdenCerradaException(
                    "La orden de mantenimiento ya se encuentra cerrada (OMT-01): " + id);
        }
        if (fechaCierre == null) {
            throw new IllegalArgumentException("La fecha de cierre no puede ser nula");
        }
        this.estado = EstadoDeOrden.CERRADA;
        this.fechaCierre = fechaCierre;
    }

    public Dinero costoTotal() {
        Dinero total = Dinero.cero(this.codigoMoneda);
        for (TrabajoRealizado trabajo : this.trabajos) {
            if (trabajo.getCostoManoDeObra() != null) {
                total = total.sumar(trabajo.getCostoManoDeObra());
            }
        }
        return total;
    }

    public String getId() {
        return id;
    }

    public String getUnidadId() {
        return unidadId;
    }

    public TipoDeMantenimiento getTipo() {
        return tipo;
    }

    public Kilometraje getKmAtencion() {
        return kmAtencion;
    }

    public EstadoDeOrden getEstado() {
        return estado;
    }

    public List<TrabajoRealizado> getTrabajos() {
        return List.copyOf(trabajos);
    }

    public LocalDate getFechaApertura() {
        return fechaApertura;
    }

    public LocalDate getFechaCierre() {
        return fechaCierre;
    }

    public String getCodigoMoneda() {
        return codigoMoneda;
    }
}
