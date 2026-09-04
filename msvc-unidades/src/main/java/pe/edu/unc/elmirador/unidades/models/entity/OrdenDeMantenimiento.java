package pe.edu.unc.elmirador.unidades.models.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import pe.edu.unc.elmirador.unidades.exceptions.KilometrajeDeAtencionInvalidoException;
import pe.edu.unc.elmirador.unidades.exceptions.OrdenCerradaException;
import pe.edu.unc.elmirador.unidades.models.vo.Dinero;
import pe.edu.unc.elmirador.unidades.models.vo.EstadoDeOrden;
import pe.edu.unc.elmirador.unidades.models.vo.Kilometraje;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeMantenimiento;

@Entity
@Table(name = "ordenes_mantenimiento")
public class OrdenDeMantenimiento {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "unidad_id", length = 40, nullable = false)
    private String unidadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_mantenimiento", length = 20, nullable = false)
    private TipoDeMantenimiento tipo;

    @Embedded
    @AttributeOverride(name = "valor", column = @Column(name = "km_atencion", nullable = false))
    private Kilometraje kmAtencion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoDeOrden estado;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "orden_id", nullable = false)
    private List<TrabajoRealizado> trabajos = new ArrayList<>();

    @Column(name = "fecha_apertura", nullable = false)
    private LocalDate fechaApertura;

    @Column(name = "fecha_cierre")
    private LocalDate fechaCierre;

    @Column(name = "codigo_moneda", length = 3, nullable = false)
    private String codigoMoneda;

    /** Exigido por JPA. No usar: no valida ninguna invariante. */
    protected OrdenDeMantenimiento() {
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrdenDeMantenimiento that = (OrdenDeMantenimiento) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
