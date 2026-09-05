package pe.edu.unc.elmirador.ejecucion.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TransbordoRequest(
        @NotBlank String nuevaUnidadId
) {
}
