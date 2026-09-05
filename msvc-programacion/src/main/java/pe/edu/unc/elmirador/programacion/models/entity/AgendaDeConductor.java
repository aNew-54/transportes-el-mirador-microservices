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
@Table(name = "agendas_conductores")
public class AgendaDeConductor {

    @Id
    @Column(name = "conductor_id", length = 40, nullable = false)
    private String conductorId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "conductor_id", nullable = false)
    private List<ReservaDeConductor> reservas = new ArrayList<>();

    /** Exigido por JPA. No usar: no valida ninguna invariante. */
    protected AgendaDeConductor() {
    }

    public AgendaDeConductor(String conductorId) {
        this(conductorId, new ArrayList<>());
    }

    public AgendaDeConductor(String conductorId, List<ReservaDeConductor> reservasIniciales) {
        if (conductorId == null || conductorId.isBlank()) {
            throw new IllegalArgumentException("El conductorId es obligatorio");
        }
        this.conductorId = conductorId.trim();
        if (reservasIniciales != null) {
            this.reservas.addAll(reservasIniciales);
        }
    }

    public String conductorId() {
        return conductorId;
    }

    public String getId() {
        return conductorId;
    }

    public List<ReservaDeConductor> reservas() {
        return List.copyOf(reservas);
    }

    public List<ReservaDeConductor> reservasQueBloquean() {
        return reservas.stream()
                .filter(r -> r.estado().bloqueaElRecurso())
                .toList();
    }

    public ReservaDeConductor reservar(
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
                    "El conductor " + conductorId + " ya tiene una reserva activa que se solapa con la ventana: " + ventana
            );
        }

        String idNormalizado = reservaId.trim();
        boolean existe = reservas.stream().anyMatch(r -> r.id().equals(idNormalizado));
        if (existe) {
            throw new DominioProgramacionException("Ya existe una reserva con id: " + idNormalizado);
        }

        ReservaDeConductor nuevaReserva = new ReservaDeConductor(
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
        ReservaDeConductor reserva = buscarReserva(reservaId.trim());
        reserva.confirmar();
    }

    public void liberar(String reservaId) {
        if (reservaId == null || reservaId.isBlank()) {
            throw new IllegalArgumentException("El id de la reserva es obligatorio");
        }
        ReservaDeConductor reserva = buscarReserva(reservaId.trim());
        reserva.liberar();
    }

    private ReservaDeConductor buscarReserva(String reservaId) {
        return reservas.stream()
                .filter(r -> r.id().equals(reservaId))
                .findFirst()
                .orElseThrow(() -> new DominioProgramacionException(
                        "No se encontro la reserva " + reservaId + " en el conductor " + conductorId
                ));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AgendaDeConductor that = (AgendaDeConductor) o;
        return Objects.equals(conductorId, that.conductorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conductorId);
    }
}
