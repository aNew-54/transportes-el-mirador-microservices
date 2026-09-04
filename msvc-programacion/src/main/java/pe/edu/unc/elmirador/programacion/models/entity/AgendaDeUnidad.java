package pe.edu.unc.elmirador.programacion.models.entity;

import java.util.ArrayList;
import java.util.List;
import pe.edu.unc.elmirador.programacion.exceptions.DominioProgramacionException;
import pe.edu.unc.elmirador.programacion.exceptions.RecursoNoElegibleException;
import pe.edu.unc.elmirador.programacion.exceptions.ReservaSolapadaException;
import pe.edu.unc.elmirador.programacion.models.vo.ElegibilidadDeRecurso;
import pe.edu.unc.elmirador.programacion.models.vo.EstadoDeReserva;
import pe.edu.unc.elmirador.programacion.models.vo.VentanaDeTiempo;

public class AgendaDeUnidad {

    private final String unidadId;
    private final List<ReservaDeUnidad> reservas;

    public AgendaDeUnidad(String unidadId) {
        this(unidadId, new ArrayList<>());
    }

    public AgendaDeUnidad(String unidadId, List<ReservaDeUnidad> reservasIniciales) {
        if (unidadId == null || unidadId.isBlank()) {
            throw new IllegalArgumentException("El unidadId es obligatorio");
        }
        this.unidadId = unidadId.trim();
        this.reservas = new ArrayList<>();
        if (reservasIniciales != null) {
            this.reservas.addAll(reservasIniciales);
        }
    }

    public String unidadId() {
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
}
