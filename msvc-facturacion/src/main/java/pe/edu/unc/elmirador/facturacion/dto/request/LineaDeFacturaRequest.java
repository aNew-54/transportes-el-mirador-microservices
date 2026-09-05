package pe.edu.unc.elmirador.facturacion.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pe.edu.unc.elmirador.facturacion.models.vo.ConceptoFacturable;

public record LineaDeFacturaRequest(
    @NotNull ConceptoFacturable concepto,
    @NotBlank String descripcion,
    @NotNull BigDecimal importeMonto
) {}
