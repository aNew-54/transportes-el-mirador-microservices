package pe.edu.unc.elmirador.ejecucion.dto.response;

import java.time.OffsetDateTime;

public record ComprobanteResponse(
        String tipo,
        String numero,
        OffsetDateTime fecha
) {
}
