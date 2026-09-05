package pe.edu.unc.elmirador.cobranza.dto.internal.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CondicionDePagoRequest(
        @NotBlank String modalidad,
        @NotNull Integer plazoEnDias
) {
}
