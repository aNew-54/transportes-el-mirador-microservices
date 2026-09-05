package pe.edu.unc.elmirador.facturacion.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AbrirFacturaRequest(
    @NotBlank String ordenDeServicioId,
    @NotBlank String clienteId,
    @Valid @NotNull SnapshotComercialRequest snapshot,
    @Valid @NotNull DetraccionRequest detraccion
) {}
