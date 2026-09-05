package pe.edu.unc.elmirador.programacion.dto.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CapacidadRequest(
        @NotNull Integer pesoMaximoKg,
        @NotNull BigDecimal volumenMaximoM3
) {}
