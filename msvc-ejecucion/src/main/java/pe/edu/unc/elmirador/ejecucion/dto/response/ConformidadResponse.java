package pe.edu.unc.elmirador.ejecucion.dto.response;

import java.time.OffsetDateTime;

import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoConformidad;

public record ConformidadResponse(
        EstadoConformidad estado,
        String recibidoPor,
        OffsetDateTime momento,
        String observaciones
) {
}
