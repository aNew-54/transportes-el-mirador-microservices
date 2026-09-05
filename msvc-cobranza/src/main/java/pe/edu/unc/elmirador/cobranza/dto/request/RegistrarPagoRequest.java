package pe.edu.unc.elmirador.cobranza.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import pe.edu.unc.elmirador.cobranza.models.vo.ModalidadDePago;

/**
 * Alta de un pago del cliente.
 *
 * <p>{@code fecha} es obligatoria y NO la pone el reloj. La fecha de un pago es un hecho —la del
 * deposito o la de la transferencia—, no el instante en que alguien lo captura en el sistema, y de
 * ella depende como envejece la cuenta para CCC-01. Ponerla con {@code now()} seria un valor por
 * defecto silencioso que adelanta o atrasa la deuda segun cuando se teclee.
 */
public record RegistrarPagoRequest(

        @NotBlank
        String clienteId,

        @NotNull @Positive
        BigDecimal montoMonto,

        @NotBlank
        String montoMoneda,

        @NotNull
        ModalidadDePago modalidad,

        String referencia,

        @NotNull
        LocalDate fecha
) {
}
