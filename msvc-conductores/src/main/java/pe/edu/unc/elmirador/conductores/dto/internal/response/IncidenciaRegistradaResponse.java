package pe.edu.unc.elmirador.conductores.dto.internal.response;

/**
 * Respuesta al reporte de incidencia del contrato 6.
 *
 * <p>El contrato no fija el cuerpo. Se devuelve el identificador asignado para que un reintento con la
 * misma {@code Idempotency-Key} pueda comprobarse contra el mismo recurso.
 */
public record IncidenciaRegistradaResponse(
        String incidenciaId,
        String conductorId,
        String viajeId
) {
}
