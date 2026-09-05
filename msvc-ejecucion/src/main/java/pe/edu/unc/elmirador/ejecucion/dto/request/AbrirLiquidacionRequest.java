package pe.edu.unc.elmirador.ejecucion.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AbrirLiquidacionRequest(
        @NotBlank String viajeId,
        @NotBlank String conductorId,
        @NotNull @PositiveOrZero BigDecimal anticipoMonto,
        @NotBlank String anticipoMoneda
) {
}
