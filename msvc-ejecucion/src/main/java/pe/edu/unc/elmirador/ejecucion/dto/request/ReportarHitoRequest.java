package pe.edu.unc.elmirador.ejecucion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pe.edu.unc.elmirador.ejecucion.models.vo.TipoDeHito;

public record ReportarHitoRequest(
        @NotNull TipoDeHito tipo,
        @NotBlank String ubicacion
) {
}
