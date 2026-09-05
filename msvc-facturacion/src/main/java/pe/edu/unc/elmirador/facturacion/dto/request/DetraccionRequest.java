package pe.edu.unc.elmirador.facturacion.dto.request;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;

public record DetraccionRequest(
    @NotNull BigDecimal porcentaje,
    @NotNull BigDecimal monto,
    String cuentaBancaria
) {}
