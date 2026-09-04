package pe.edu.unc.elmirador.programacion.models.entity;

import pe.edu.unc.elmirador.programacion.exceptions.DominioProgramacionException;
import pe.edu.unc.elmirador.programacion.models.vo.EstadoDeReserva;
import pe.edu.unc.elmirador.programacion.models.vo.VentanaDeTiempo;

public class ReservaDeUnidad {

    private final String id;
    private final String viajeId;
    private final VentanaDeTiempo ventana;
    private EstadoDeReserva estado;

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

    public String viajeId() {
        return viajeId;
    }

    public VentanaDeTiempo ventana() {
        return ventana;
    }

    public EstadoDeReserva estado() {
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
}
