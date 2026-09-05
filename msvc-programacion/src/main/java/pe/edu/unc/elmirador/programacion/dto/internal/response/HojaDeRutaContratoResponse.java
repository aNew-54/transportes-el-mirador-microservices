package pe.edu.unc.elmirador.programacion.dto.internal.response;

import java.util.List;

/**
 * Contrato 4 · Ejecucion → Programacion.
 *
 * <p>El orden de {@code paradas} ya viene resuelto por VIA-06: la carga que se descarga primero se
 * estiba al final. Ejecucion no lo recalcula, y este endpoint tampoco: se serializa en el orden en
 * que el agregado las tiene.
 */
public record HojaDeRutaContratoResponse(
        String viajeId,
        String estado,
        String unidadId,
        List<String> conductorIds,
        String observaciones,
        List<ParadaContratoResponse> paradas) {
}
