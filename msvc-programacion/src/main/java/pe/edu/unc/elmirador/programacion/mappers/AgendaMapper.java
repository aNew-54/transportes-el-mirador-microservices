package pe.edu.unc.elmirador.programacion.mappers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import pe.edu.unc.elmirador.programacion.dto.response.AgendaDeConductorResponse;
import pe.edu.unc.elmirador.programacion.dto.response.AgendaDeUnidadResponse;
import pe.edu.unc.elmirador.programacion.dto.response.ReservaDeConductorResponse;
import pe.edu.unc.elmirador.programacion.dto.response.ReservaDeUnidadResponse;
import pe.edu.unc.elmirador.programacion.dto.response.VentanaDeTiempoResponse;
import pe.edu.unc.elmirador.programacion.models.entity.AgendaDeConductor;
import pe.edu.unc.elmirador.programacion.models.entity.AgendaDeUnidad;
import pe.edu.unc.elmirador.programacion.models.entity.ReservaDeConductor;
import pe.edu.unc.elmirador.programacion.models.entity.ReservaDeUnidad;
import pe.edu.unc.elmirador.programacion.models.vo.VentanaDeTiempo;

public final class AgendaMapper {

    private AgendaMapper() {
    }

    public static AgendaDeUnidadResponse aResponse(AgendaDeUnidad agenda) {
        if (agenda == null) return null;
        List<ReservaDeUnidadResponse> reservas = new ArrayList<>();
        for (ReservaDeUnidad r : agenda.reservas()) {
            reservas.add(aResponse(r));
        }
        return new AgendaDeUnidadResponse(agenda.unidadId(), reservas);
    }

    public static ReservaDeUnidadResponse aResponse(ReservaDeUnidad reserva) {
        if (reserva == null) return null;
        return new ReservaDeUnidadResponse(
                reserva.id(),
                reserva.viajeId(),
                aResponse(reserva.ventana()),
                reserva.estado()
        );
    }

    public static AgendaDeConductorResponse aResponse(AgendaDeConductor agenda) {
        if (agenda == null) return null;
        List<ReservaDeConductorResponse> reservas = new ArrayList<>();
        for (ReservaDeConductor r : agenda.reservas()) {
            reservas.add(aResponse(r));
        }
        return new AgendaDeConductorResponse(agenda.conductorId(), reservas);
    }

    public static ReservaDeConductorResponse aResponse(ReservaDeConductor reserva) {
        if (reserva == null) return null;
        return new ReservaDeConductorResponse(
                reserva.id(),
                reserva.viajeId(),
                aResponse(reserva.ventana()),
                reserva.estado()
        );
    }

    private static VentanaDeTiempoResponse aResponse(VentanaDeTiempo ventana) {
        if (ventana == null) return null;
        return new VentanaDeTiempoResponse(ventana.desde(), ventana.hasta());
    }
}
