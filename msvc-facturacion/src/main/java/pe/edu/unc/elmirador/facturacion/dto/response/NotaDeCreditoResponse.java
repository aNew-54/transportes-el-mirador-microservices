package pe.edu.unc.elmirador.facturacion.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import pe.edu.unc.elmirador.facturacion.models.vo.MotivoDeAjuste;

public record NotaDeCreditoResponse(
    String id,
    String facturaId,
    MotivoDeAjuste motivo,
    BigDecimal montoMonto,
    String montoMoneda,
    OffsetDateTime fechaDeEmision,
    String motivoDetalle
) {}
