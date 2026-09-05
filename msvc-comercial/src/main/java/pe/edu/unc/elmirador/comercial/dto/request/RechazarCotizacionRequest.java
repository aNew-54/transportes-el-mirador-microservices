package pe.edu.unc.elmirador.comercial.dto.request;

import jakarta.validation.constraints.NotNull;
import pe.edu.unc.elmirador.comercial.models.vo.MotivoDeRechazo;

public record RechazarCotizacionRequest(
        @NotNull MotivoDeRechazo motivo
) {
}
