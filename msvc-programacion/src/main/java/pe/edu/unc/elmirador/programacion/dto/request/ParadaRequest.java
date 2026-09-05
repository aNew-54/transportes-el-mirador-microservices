package pe.edu.unc.elmirador.programacion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record ParadaRequest(
        @NotNull Integer secuencia,
        @NotBlank String tipo,
        @NotBlank String ordenDeServicioId,
        String ubicacion,
        OffsetDateTime horaEstimada
) {}
