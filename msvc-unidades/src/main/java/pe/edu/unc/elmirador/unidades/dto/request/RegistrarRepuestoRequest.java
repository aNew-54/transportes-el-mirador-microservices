package pe.edu.unc.elmirador.unidades.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record RegistrarRepuestoRequest(
        @NotBlank String codigo,
        @NotBlank String descripcion,
        @PositiveOrZero int existencias,
        @PositiveOrZero int stockMinimo,
        @NotNull @PositiveOrZero BigDecimal costoUnitario,
        @NotBlank String moneda
) {
}
