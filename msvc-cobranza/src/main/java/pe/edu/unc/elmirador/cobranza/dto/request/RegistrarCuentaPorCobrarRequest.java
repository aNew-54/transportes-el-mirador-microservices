package pe.edu.unc.elmirador.cobranza.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record RegistrarCuentaPorCobrarRequest(
        @NotBlank String clienteId,
        @NotBlank String facturaId,
        @NotBlank String documentoId,
        @NotNull @PositiveOrZero BigDecimal totalMonto,
        @NotBlank String totalMoneda,
        @NotNull @PositiveOrZero BigDecimal detraccionMonto,
        @NotBlank String detraccionMoneda,
        @NotNull @PositiveOrZero BigDecimal montoNetoMonto,
        @NotBlank String montoNetoMoneda,
        @NotNull LocalDate fechaDeVencimiento
) {
}
