package pe.edu.unc.elmirador.ejecucion.clients.dto;

import java.time.OffsetDateTime;

public record KilometrajePeticion(
        String viajeId,
        int kilometraje,
        OffsetDateTime momento
) {
}
