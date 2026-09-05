package pe.edu.unc.elmirador.programacion.models.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import pe.edu.unc.elmirador.programacion.exceptions.DominioProgramacionException;
import pe.edu.unc.elmirador.programacion.exceptions.RecursoNoElegibleException;
import pe.edu.unc.elmirador.programacion.exceptions.ReservaSolapadaException;
import pe.edu.unc.elmirador.programacion.models.vo.ElegibilidadDeRecurso;
import pe.edu.unc.elmirador.programacion.models.vo.EstadoDeReserva;
import pe.edu.unc.elmirador.programacion.models.vo.VentanaDeTiempo;

@Entity
@Table(name = "agendas_unidades")
public class AgendaDeUnidad {

    @Id
    @Column(name = "unidad_id", length = 40, nullable = false)
    private String unidadId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "unidad_id", nullable = false)
    private List<ReservaDeUnidad> reservas = new ArrayList<>();

    /** Exigido por JPA. No usar: no valida ninguna invariante. */
    protected AgendaDeUnidad() {
    }

    public AgendaDeUnidad(String unidadId) {
        this(unidadId, new ArrayList<>());
    }

    public AgendaDeUnidad(String unidadId, List<ReservaDeUnidad> reservasIniciales) {
        if (unidadId == null || unidadId.isBlank()) {
            throw new IllegalArgumentException("El unidadId es obligatorio");
        }
        this.unidadId = unidadId.trim();
        if (reservasIniciales != null) {
            this.reservas.addAll(reservasIniciales);
        }
    }

    public String unidadId() {
        return unidadId;
    }

    public String getId() {
        return unidadId;
    }

    public List<ReservaDeUnidad> reservas() {
        return List.copyOf(reservas);
    }

    public List<ReservaDeUnidad> reservasQueBloquean() {
        return reservas.stream()
                .filter(r -> r.estado().bloqueaElRecurso())
                .toList();
    }

    public ReservaDeUnidad reservar(
            String reservaId,
            VentanaDeTiempo ventana,
            ElegibilidadDeRecurso elegibilidad,
            String viajeId) {
        if (reservaId == null || reservaId.isBlank()) {
            throw new IllegalArgumentException("El id de la reserva es obligatorio");
        }
        if (ventana == null) {
            throw new IllegalArgumentException("La ventana de tiempo es obligatoria");
        }
        if (elegibilidad == null) {
            throw new IllegalArgumentException("La elegibilidad del recurso es obligatoria");
        }
        if (viajeId == null || viajeId.isBlank()) {
            throw new IllegalArgumentException("El viajeId es obligatorio");
        }

        if (!elegibilidad.elegible()) {
            throw new RecursoNoElegibleException(elegibilidad.motivos());
        }

        boolean seSolapa = reservasQueBloquean().stream()
                .anyMatch(r -> r.ventana().seSolapaCon(ventana));
        if (seSolapa) {
            throw new ReservaSolapadaException(
                    "La unidad " + unidadId + " ya tiene una reserva activa que se solapa con la ventana: " + ventana
            );
        }

        String idNormalizado = reservaId.trim();
        boolean existe = reservas.stream().anyMatch(r -> r.id().equals(idNormalizado));
        if (existe) {
            throw new DominioProgramacionException("Ya existe una reserva con id: " + idNormalizado);
        }

        ReservaDeUnidad nuevaReserva = new ReservaDeUnidad(
                idNormalizado,
                viajeId.trim(),
                ventana,
                EstadoDeReserva.TENTATIVA
        );
        this.reservas.add(nuevaReserva);
        return nuevaReserva;
    }

    public void confirmar(String reservaId) {
        if (reservaId == null || reservaId.isBlank()) {
            throw new IllegalArgumentException("El id de la reserva es obligatorio");
        }
        ReservaDeUnidad reserva = buscarReserva(reservaId.trim());
        reserva.confirmar();
    }

    public void liberar(String reservaId) {
        if (reservaId == null || reservaId.isBlank()) {
            throw new IllegalArgumentException("El id de la reserva es obligatorio");
        }
        ReservaDeUnidad reserva = buscarReserva(reservaId.trim());
        reserva.liberar();
    }

    private ReservaDeUnidad buscarReserva(String reservaId) {
        return reservas.stream()
                .filter(r -> r.id().equals(reservaId))
                .findFirst()
                .orElseThrow(() -> new DominioProgramacionException(
                        "No se encontro la reserva " + reservaId + " en la unidad " + unidadId
                ));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgendaDeUnidad that = (AgendaDeUnidad) o;
        return Objects.equals(unidadId, that.unidadId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(unidadId);
    }
}
