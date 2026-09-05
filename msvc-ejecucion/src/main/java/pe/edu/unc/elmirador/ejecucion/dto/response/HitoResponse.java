package pe.edu.unc.elmirador.ejecucion.dto.response;

import java.time.OffsetDateTime;

import pe.edu.unc.elmirador.ejecucion.models.vo.TipoDeHito;

public record HitoResponse(
        TipoDeHito tipo,
        OffsetDateTime momento,
        String ubicacion
) {
}
