package pe.edu.unc.elmirador.cobranza.dto.internal.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record DetraccionRequest(
        @NotNull BigDecimal porcentaje,
        @NotBlank String monto,
        @NotBlank String moneda,
        @NotBlank String cuentaBancaria
) {
}
