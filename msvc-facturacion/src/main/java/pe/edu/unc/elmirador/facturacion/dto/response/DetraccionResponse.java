package pe.edu.unc.elmirador.facturacion.dto.response;

import java.math.BigDecimal;

public record DetraccionResponse(
    BigDecimal porcentaje,
    BigDecimal montoMonto,
    String montoMoneda,
    String cuentaBancaria
) {}
