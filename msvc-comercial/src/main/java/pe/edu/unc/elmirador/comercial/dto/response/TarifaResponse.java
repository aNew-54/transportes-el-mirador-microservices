package pe.edu.unc.elmirador.comercial.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record TarifaResponse(
        BigDecimal baseMonto,
        String baseMoneda,
        List<RecargoResponse> recargos,
        DescuentoResponse descuento
) {
    public record RecargoResponse(
            String tipo,
            BigDecimal porcentaje
    ) {
    }

    public record DescuentoResponse(
            BigDecimal porcentaje,
            String autorizadoPor
    ) {
    }
}
