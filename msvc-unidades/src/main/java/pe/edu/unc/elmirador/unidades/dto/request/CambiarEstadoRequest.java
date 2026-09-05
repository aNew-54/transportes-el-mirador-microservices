package pe.edu.unc.elmirador.unidades.dto.request;

import jakarta.validation.constraints.NotNull;
import pe.edu.unc.elmirador.unidades.models.vo.SituacionOperativa;

public record CambiarEstadoRequest(
        @NotNull SituacionOperativa situacion,
        String motivo
) {
}
