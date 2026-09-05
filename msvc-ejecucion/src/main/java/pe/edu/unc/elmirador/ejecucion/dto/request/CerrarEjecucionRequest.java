package pe.edu.unc.elmirador.ejecucion.dto.request;

import jakarta.validation.constraints.NotNull;

public record CerrarEjecucionRequest(
        @NotNull Boolean hayLiquidacionesPendientes
) {
}
