package pe.edu.unc.elmirador.unidades.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record RegistrarTrabajoRequest(
        @NotBlank String descripcion,
        @NotNull @PositiveOrZero BigDecimal costoManoDeObra,
        @NotBlank String monedaManoDeObra,
        String repuestoId,
        @PositiveOrZero Integer cantidadRepuesto
) {
}
