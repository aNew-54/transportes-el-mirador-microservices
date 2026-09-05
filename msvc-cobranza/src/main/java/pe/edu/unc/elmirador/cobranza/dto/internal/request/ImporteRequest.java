package pe.edu.unc.elmirador.cobranza.dto.internal.request;

import jakarta.validation.constraints.NotBlank;

public record ImporteRequest(
        @NotBlank String monto,
        @NotBlank String moneda
) {
}
