package pe.edu.unc.elmirador.facturacion.dto.request;

import java.math.BigDecimal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmitirFalsoFleteRequest(
    @NotBlank String ordenDeServicioId,
    @NotBlank String clienteId,
    @Valid @NotNull SnapshotComercialRequest snapshot,
    @Valid @NotNull DetraccionRequest detraccion,
    @NotBlank String serie,
    @NotNull Integer correlativo,
    @NotBlank String descripcionLinea,
    @NotNull BigDecimal importeMonto
) {}
