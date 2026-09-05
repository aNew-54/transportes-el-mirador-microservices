package pe.edu.unc.elmirador.unidades.dto.internal.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record ReportarFallaRequest(
        @NotBlank String viajeId,
        @NotBlank String tipo,
        @NotBlank String descripcion,
        @NotNull OffsetDateTime momento,
        @NotNull Boolean dejaInoperativa
) {
}
