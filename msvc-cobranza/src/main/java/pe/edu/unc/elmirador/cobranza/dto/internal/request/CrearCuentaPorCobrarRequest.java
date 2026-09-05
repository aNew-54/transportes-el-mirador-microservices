package pe.edu.unc.elmirador.cobranza.dto.internal.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record CrearCuentaPorCobrarRequest(
        @NotBlank String facturaId,
        @NotBlank String documentoId,
        @NotBlank String clienteId,
        @NotNull @Valid ImporteRequest total,
        @NotNull @Valid DetraccionRequest detraccion,
        @NotNull @Valid ImporteRequest montoNeto,
        @NotNull OffsetDateTime fechaDeEmision,
        @NotNull OffsetDateTime fechaDeVencimiento,
        @NotNull @Valid CondicionDePagoRequest condicionDePago
) {
}
