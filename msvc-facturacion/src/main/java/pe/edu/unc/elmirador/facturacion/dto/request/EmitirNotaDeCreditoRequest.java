package pe.edu.unc.elmirador.facturacion.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pe.edu.unc.elmirador.facturacion.models.vo.MotivoDeAjuste;

public record EmitirNotaDeCreditoRequest(
    @NotBlank String facturaId,
    @NotNull MotivoDeAjuste motivo,
    @NotNull BigDecimal monto,
    String motivoDetalle
) {}
