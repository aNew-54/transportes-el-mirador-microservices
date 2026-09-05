package pe.edu.unc.elmirador.ejecucion.clients.dto;

import java.time.OffsetDateTime;

public record HorasConduccionPeticion(
        String viajeId,
        double horas,
        OffsetDateTime desde,
        OffsetDateTime hasta
) {
}
