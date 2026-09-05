package pe.edu.unc.elmirador.programacion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Donde para el camion. Solo la direccion es obligatoria: un punto de carga habitual puede no tener
 * referencia ni contacto, y exigirlos obligaria a rellenarlos con algo.
 */
public record UbicacionRequest(
        @NotBlank @Size(max = 300) String direccion,
        @Size(max = 100) String distrito,
        @Size(max = 200) String referencia,
        @Size(max = 50) String contacto
) {
}
