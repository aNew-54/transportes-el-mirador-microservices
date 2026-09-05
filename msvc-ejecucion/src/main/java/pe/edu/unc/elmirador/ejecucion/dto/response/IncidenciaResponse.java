package pe.edu.unc.elmirador.ejecucion.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

import pe.edu.unc.elmirador.ejecucion.models.vo.TipoDeIncidencia;

public record IncidenciaResponse(
        TipoDeIncidencia tipo,
        String descripcion,
        EvidenciaResponse evidencia,
        Boolean resuelta,
        OffsetDateTime momento
) {
}
