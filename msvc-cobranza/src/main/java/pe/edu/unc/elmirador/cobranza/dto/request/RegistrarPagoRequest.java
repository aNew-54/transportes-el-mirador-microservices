package pe.edu.unc.elmirador.cobranza.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import pe.edu.unc.elmirador.cobranza.models.vo.ModalidadDePago;

public record RegistrarPagoRequest(
        @NotBlank String clienteId,
        @NotNull @Positive BigDecimal montoMonto,
        @NotBlank String montoMoneda,
        @NotNull ModalidadDePago modalidad,
        String referencia
) {
}
