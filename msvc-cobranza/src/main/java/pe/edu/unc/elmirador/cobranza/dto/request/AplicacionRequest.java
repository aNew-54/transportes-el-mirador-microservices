package pe.edu.unc.elmirador.cobranza.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AplicacionRequest(
        @NotBlank String cuentaPorCobrarId,
        @NotNull @Positive BigDecimal importeMonto,
        @NotBlank String importeMoneda
) {
}
