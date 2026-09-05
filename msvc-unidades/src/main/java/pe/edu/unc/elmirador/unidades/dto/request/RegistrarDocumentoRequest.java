package pe.edu.unc.elmirador.unidades.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeDocumento;

public record RegistrarDocumentoRequest(
        @NotNull TipoDeDocumento tipoDocumento,
        @NotNull LocalDate desde,
        @NotNull LocalDate hasta,
        @NotBlank String numero
) {
}
