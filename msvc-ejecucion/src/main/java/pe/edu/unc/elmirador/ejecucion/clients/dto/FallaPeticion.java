package pe.edu.unc.elmirador.ejecucion.clients.dto;

import java.time.OffsetDateTime;

public record FallaPeticion(
        String viajeId,
        String tipo,
        String descripcion,
        OffsetDateTime momento,
        boolean dejaInoperativa
) {
}
