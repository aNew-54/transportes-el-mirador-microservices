package pe.edu.unc.elmirador.ejecucion.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Abrir la ejecucion de un viaje ya programado.
 *
 * <p>Antes traia tambien la unidad ejecutora y la lista de paradas. Las dos son de la hoja de ruta,
 * que es de Programacion: quien abria la ejecucion podia declarar otra unidad y otras paradas, y
 * Ejecucion habria seguido esa version. Ahora las trae el contrato 4, y si Programacion no responde
 * la ejecucion no se abre.
 */
public record CrearEjecucionRequest(@NotBlank String viajeId) {
}
