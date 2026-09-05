package pe.edu.unc.elmirador.facturacion.dto.response;

import java.math.BigDecimal;
import pe.edu.unc.elmirador.facturacion.models.vo.ConceptoFacturable;

public record LineaDeFacturaResponse(
    String id,
    String ordenDeServicioId,
    ConceptoFacturable concepto,
    String descripcion,
    BigDecimal importeMonto,
    String importeMoneda
) {}
