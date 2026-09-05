package pe.edu.unc.elmirador.comercial.dto.internal.response;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Contrato 9: Snapshot facturable
 */
public record SnapshotFacturableResponse(
        String ordenId,
        String clienteId,
        String ruc,
        String razonSocial,
        TarifaResponse tarifa,
        CondicionDePagoResponse condicionDePago,
        OffsetDateTime tomadoEn
) {
    public record TarifaResponse(
            DineroResponse fleteBase,
            List<RecargoResponse> recargos,
            DescuentoResponse descuento,
            DineroResponse total
    ) {}

    public record DineroResponse(
            String monto,
            String moneda
    ) {}

    public record RecargoResponse(
            String tipo,
            java.math.BigDecimal porcentaje
    ) {}

    public record DescuentoResponse(
            java.math.BigDecimal porcentaje,
            String motivo
    ) {}

    public record CondicionDePagoResponse(
            String modalidad,
            int plazoEnDias
    ) {}
}
