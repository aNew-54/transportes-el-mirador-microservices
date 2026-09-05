package pe.edu.unc.elmirador.ejecucion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ParadaRequest(
        @NotNull @Positive Integer secuencia,
        @NotBlank String ordenDeServicioId,
        @NotBlank String direccion
) {
}
