package pe.edu.unc.elmirador.conductores.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Acumulado de conduccion en la ventana vigente (CON-02).
 *
 * <p>{@code disponibles} es derivado y se calcula al vuelo (regla D8): nunca se almacena.
 */
public record HorasResponse(
        String conductorId,
        BigDecimal acumuladas,
        BigDecimal disponibles,
        BigDecimal maximoNormado,
        LocalDate ventanaDesde,
        LocalDate ventanaHasta
) {
}
