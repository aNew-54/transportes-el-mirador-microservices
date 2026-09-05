package pe.edu.unc.elmirador.ejecucion.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.unc.elmirador.ejecucion.models.vo.TipoDeIncidencia;

public record RegistrarIncidenciaRequest(
        @NotNull TipoDeIncidencia tipo,
        @NotBlank String descripcion,
        @NotNull @Size(max = 10) List<String> fotografias
) {
}
