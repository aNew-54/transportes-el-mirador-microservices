package pe.edu.unc.elmirador.unidades.dto.request;

import jakarta.validation.constraints.NotNull;

public record AjustarInventarioRequest(
        @NotNull int cantidad
) {
}
