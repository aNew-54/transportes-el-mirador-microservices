package pe.edu.unc.elmirador.ejecucion.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record CheckListResponse(
        Boolean aprobado,
        List<String> observaciones,
        OffsetDateTime momento
) {
}
