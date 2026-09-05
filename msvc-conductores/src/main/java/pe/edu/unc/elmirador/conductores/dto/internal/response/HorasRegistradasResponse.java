package pe.edu.unc.elmirador.conductores.dto.internal.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Respuesta al reporte de horas del contrato 6.
 *
 * <p>El contrato no fija el cuerpo de la respuesta, solo los codigos. Se devuelve el acumulado
 * resultante porque es lo que Ejecucion necesita para saber si al conductor le quedan horas para el
 * siguiente tramo, y porque un cuerpo vacio obligaria a una segunda llamada.
 */
public record HorasRegistradasResponse(
        String conductorId,
        String viajeId,
        BigDecimal horasAcumuladas,
        BigDecimal horasDisponibles,
        LocalDate ventanaDesde
) {
}
