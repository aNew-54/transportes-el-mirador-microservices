package pe.edu.unc.elmirador.unidades.dto.internal.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record ReportarKilometrajeRequest(
        @NotBlank String viajeId,
        @NotNull Integer kilometraje,
        @NotNull OffsetDateTime momento
) {
}
