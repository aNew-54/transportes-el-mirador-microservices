package pe.edu.unc.elmirador.comercial.dto.internal.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Un importe con su moneda, tal como lo escribe la regla 2 de los contratos. */
public record ImporteRequest(

        @NotNull
        BigDecimal monto,

        @NotBlank
        @Size(min = 3, max = 3)
        String moneda
) {
}
