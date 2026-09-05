package pe.edu.unc.elmirador.programacion.dto.response;

import java.util.List;

public record AgendaDeUnidadResponse(
        String unidadId,
        List<ReservaDeUnidadResponse> reservas
) {}
