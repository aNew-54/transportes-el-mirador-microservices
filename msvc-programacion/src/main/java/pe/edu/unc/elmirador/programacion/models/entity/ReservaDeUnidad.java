package pe.edu.unc.elmirador.programacion.models.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import pe.edu.unc.elmirador.programacion.exceptions.DominioProgramacionException;
import pe.edu.unc.elmirador.programacion.models.vo.EstadoDeReserva;
import pe.edu.unc.elmirador.programacion.models.vo.VentanaDeTiempo;

@Entity
@Table(name = "reservas_unidades")
public class ReservaDeUnidad {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "viaje_id", length = 40, nullable = false)
    private String viajeId;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "desde", column = @Column(name = "ventana_desde", nullable = false)),
            @AttributeOverride(name = "hasta", column = @Column(name = "ventana_hasta", nullable = false))
    })
    private VentanaDeTiempo ventana;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoDeReserva estado;

    /** Exigido por JPA. No usar: no valida ninguna invariante. */
    protected ReservaDeUnidad() {
    }

    public ReservaDeUnidad(String id, String viajeId, VentanaDeTiempo ventana, EstadoDeReserva estado) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id de la reserva es obligatorio");
        }
        if (viajeId == null || viajeId.isBlank()) {
            throw new IllegalArgumentException("El viajeId es obligatorio");
        }
        if (ventana == null) {
            throw new IllegalArgumentException("La ventana de tiempo es obligatoria");
        }
        if (estado == null) {
            throw new IllegalArgumentException("El estado de reserva es obligatorio");
        }
        this.id = id.trim();
        this.viajeId = viajeId.trim();
        this.ventana = ventana;
        this.estado = estado;
    }

    public String id() {
        return id;
    }

    public String getId() {
        return id;
    }

    public String viajeId() {
        return viajeId;
    }

    public String getViajeId() {
        return viajeId;
    }

    public VentanaDeTiempo ventana() {
        return ventana;
    }

    public VentanaDeTiempo getVentana() {
        return ventana;
    }

    public EstadoDeReserva estado() {
        return estado;
    }

    public EstadoDeReserva getEstado() {
        return estado;
    }

    public void confirmar() {
        if (this.estado == EstadoDeReserva.LIBERADA) {
            throw new DominioProgramacionException("Una reserva liberada no se puede confirmar: " + id);
        }
        this.estado = EstadoDeReserva.CONFIRMADA;
    }

    public void liberar() {
        this.estado = EstadoDeReserva.LIBERADA;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReservaDeUnidad that = (ReservaDeUnidad) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
