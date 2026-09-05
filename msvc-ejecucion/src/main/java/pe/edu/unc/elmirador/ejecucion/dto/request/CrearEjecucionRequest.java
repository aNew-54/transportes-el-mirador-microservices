package pe.edu.unc.elmirador.ejecucion.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CrearEjecucionRequest(
        @NotBlank String viajeId,
        @NotBlank String unidadEjecutoraId,
        @NotEmpty @Valid List<ParadaRequest> paradas
) {
}
