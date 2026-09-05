package pe.edu.unc.elmirador.programacion.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ElegibilidadDeRecursoRequest(
        @NotNull Boolean elegible,
        @NotNull List<String> motivos
) {}
