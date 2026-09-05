package pe.edu.unc.elmirador.programacion.dto.response;

import pe.edu.unc.elmirador.programacion.models.vo.EstadoDeReserva;

public record ReservaDeConductorResponse(
        String id,
        String viajeId,
        VentanaDeTiempoResponse ventana,
        EstadoDeReserva estado
) {}
