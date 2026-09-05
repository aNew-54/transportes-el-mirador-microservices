package pe.edu.unc.elmirador.conductores.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Suspension del conductor. El motivo es obligatorio: {@code EstadoDeHabilitacion} lo exige. */
public record SuspenderConductorRequest(

        @NotBlank
        @Size(max = 300)
        String motivo
) {
}
