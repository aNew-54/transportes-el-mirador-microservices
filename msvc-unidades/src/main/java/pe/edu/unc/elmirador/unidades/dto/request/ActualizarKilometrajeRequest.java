package pe.edu.unc.elmirador.unidades.dto.request;

import jakarta.validation.constraints.PositiveOrZero;

public record ActualizarKilometrajeRequest(
        @PositiveOrZero int kilometraje
) {
}
