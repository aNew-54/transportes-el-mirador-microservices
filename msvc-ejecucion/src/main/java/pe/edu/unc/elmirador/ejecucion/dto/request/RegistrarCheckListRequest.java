package pe.edu.unc.elmirador.ejecucion.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrarCheckListRequest(
        @NotNull Boolean aprobado,
        @NotNull @Size(max = 1000) List<String> observaciones
) {
}
