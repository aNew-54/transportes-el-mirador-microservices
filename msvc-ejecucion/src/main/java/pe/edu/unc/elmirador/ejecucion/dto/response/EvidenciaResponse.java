package pe.edu.unc.elmirador.ejecucion.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record EvidenciaResponse(
        List<String> fotografias,
        String descripcion,
        OffsetDateTime momento
) {
}
