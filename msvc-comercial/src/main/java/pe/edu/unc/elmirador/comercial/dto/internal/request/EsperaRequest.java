package pe.edu.unc.elmirador.comercial.dto.internal.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record EsperaRequest(
        @NotBlank String viajeId,
        @NotBlank String punto,
        @NotNull @PositiveOrZero BigDecimal tiempoLibreHoras,
        @NotNull @PositiveOrZero BigDecimal tiempoRealHoras,
        @NotNull @PositiveOrZero BigDecimal excedenteHoras
) {
}
