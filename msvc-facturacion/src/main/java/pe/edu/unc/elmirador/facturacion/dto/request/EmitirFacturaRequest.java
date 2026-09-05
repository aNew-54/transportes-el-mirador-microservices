package pe.edu.unc.elmirador.facturacion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmitirFacturaRequest(
    @NotBlank String serie,
    @NotNull Integer correlativo
) {}
