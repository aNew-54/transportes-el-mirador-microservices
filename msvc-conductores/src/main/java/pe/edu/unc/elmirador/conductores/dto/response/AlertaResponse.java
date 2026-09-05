package pe.edu.unc.elmirador.conductores.dto.response;

import java.time.LocalDate;

/**
 * Vencimiento proximo de una licencia o de una induccion.
 *
 * <p>{@code diasRestantes} lo calcula el mapeador contra la fecha del reloj inyectado, no contra
 * {@code LocalDate.now()}.
 */
public record AlertaResponse(
        String conductorId,
        String nombreCompleto,
        TipoDeAlerta tipo,
        String referencia,
        LocalDate venceEl,
        long diasRestantes
) {

    public enum TipoDeAlerta {
        LICENCIA,
        INDUCCION
    }
}
