package pe.edu.unc.elmirador.facturacion.clients.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record SnapshotFacturableRemoto(
        String ordenId,
        String clienteId,
        String ruc,
        String razonSocial,
        TarifaRemota tarifa,
        CondicionDePagoRemota condicionDePago,
        OffsetDateTime tomadoEn
) {
    public record TarifaRemota(
            DineroRemoto fleteBase,
            List<RecargoRemoto> recargos,
            DescuentoRemoto descuento,
            DineroRemoto total
    ) {}

    public record DineroRemoto(
            String monto,
            String moneda
    ) {}

    public record RecargoRemoto(
            String tipo,
            java.math.BigDecimal porcentaje
    ) {}

    public record DescuentoRemoto(
            java.math.BigDecimal porcentaje,
            String motivo
    ) {}

    public record CondicionDePagoRemota(
            String modalidad,
            int plazoEnDias
    ) {}
}
