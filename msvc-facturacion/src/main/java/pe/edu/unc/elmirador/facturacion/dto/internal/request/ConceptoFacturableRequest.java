package pe.edu.unc.elmirador.facturacion.dto.internal.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ConceptoFacturableRequest(
        @NotBlank String concepto,
        @NotNull BigDecimal monto,
        @NotBlank String moneda,
        String detalle
) {
}
