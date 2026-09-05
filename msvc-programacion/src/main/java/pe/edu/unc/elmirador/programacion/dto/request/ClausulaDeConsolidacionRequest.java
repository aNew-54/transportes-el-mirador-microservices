package pe.edu.unc.elmirador.programacion.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ClausulaDeConsolidacionRequest(
        @NotNull Boolean permitida,
        @NotNull List<String> restricciones
) {}
