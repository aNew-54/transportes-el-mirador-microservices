package pe.edu.unc.elmirador.programacion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Asignar unidad y conductores a un viaje.
 *
 * <p>Ya no trae la elegibilidad. La traia, y eso queria decir que quien asignaba declaraba tambien
 * que la unidad y los conductores eran elegibles: la comprobacion no podia fallar si el solicitante
 * no queria. Ahora sale de los contratos 2 y 3, que es para lo que existen.
 */
public record AsignarRecursosRequest(
        @NotBlank String unidadId,
        @NotEmpty List<@NotBlank String> conductorIds,
        @NotNull Boolean conRelevo
) {}
