package pe.edu.unc.elmirador.unidades.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegistrarFallaRequest(
        @NotNull boolean dejaInoperativa,
        @NotBlank String motivo
) {
}
