package pe.edu.unc.elmirador.facturacion.dto.request;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SnapshotComercialRequest(
    @NotNull BigDecimal tarifaMonto,
    @NotBlank String codigoMoneda,
    @NotNull OffsetDateTime obtenidoEn
) {}
