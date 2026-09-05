package pe.edu.unc.elmirador.facturacion.dto.response;

import java.math.BigDecimal;

public record DineroResponse(
    BigDecimal monto,
    String codigoMoneda
) {}
