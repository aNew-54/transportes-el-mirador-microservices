package pe.edu.unc.elmirador.programacion.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record VentanaDeTiempoRequest(
        @NotNull OffsetDateTime desde,
        @NotNull OffsetDateTime hasta
) {}
