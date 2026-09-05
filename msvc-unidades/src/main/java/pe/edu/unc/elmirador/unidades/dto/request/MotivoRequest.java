package pe.edu.unc.elmirador.unidades.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Motivo de un cambio de estado operativo. {@code EstadoOperativo} lo exige cuando la unidad sale de
 * servicio, asi que aqui es obligatorio y el {@code 400} se adelanta a la frontera.
 */
public record MotivoRequest(

        @NotBlank
        @Size(max = 300)
        String motivo
) {
}
