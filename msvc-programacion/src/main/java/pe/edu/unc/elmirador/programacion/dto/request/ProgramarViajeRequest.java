package pe.edu.unc.elmirador.programacion.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ProgramarViajeRequest(
        @NotEmpty List<@NotNull @Valid ParadaRequest> hojaDeRuta,

        /** Instrucciones de la programacion para quien ejecuta. Opcional. */
        String observaciones
) {}
