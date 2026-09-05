package pe.edu.unc.elmirador.programacion.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AsignarRecursosRequest(
        @NotBlank String unidadId,
        @NotEmpty List<@NotBlank String> conductorIds,
        @NotNull Boolean conRelevo,
        @NotNull @Valid ElegibilidadDeRecursoRequest elegibilidadDeLaUnidad,
        @NotEmpty List<@NotNull @Valid ElegibilidadDeRecursoRequest> elegibilidadDeLosConductores
) {}
