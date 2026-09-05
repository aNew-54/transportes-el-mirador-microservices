package pe.edu.unc.elmirador.facturacion.clients.dto;

import java.time.OffsetDateTime;

public record CuentaPorCobrarRequestRemoto(
        String facturaId,
        String documentoId,
        String clienteId,
        ImporteRemoto total,
        DetraccionRemoto detraccion,
        ImporteRemoto montoNeto,
        OffsetDateTime fechaDeEmision,
        OffsetDateTime fechaDeVencimiento,
        CondicionDePagoRemota condicionDePago
) {
    public record ImporteRemoto(
            String monto,
            String moneda
    ) {}

    public record DetraccionRemoto(
            java.math.BigDecimal porcentaje,
            String monto,
            String moneda,
            String cuentaBancaria
    ) {}

    public record CondicionDePagoRemota(
            String modalidad,
            int plazoEnDias
    ) {}
}
