package pe.edu.unc.elmirador.ejecucion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoConformidad;

public record ConformidadRequest(
        @NotNull EstadoConformidad estado,
        @NotBlank String recibidoPor,
        String observaciones
) {
}
