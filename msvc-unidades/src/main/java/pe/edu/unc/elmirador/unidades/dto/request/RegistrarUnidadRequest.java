package pe.edu.unc.elmirador.unidades.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import pe.edu.unc.elmirador.unidades.models.vo.IntervaloDeMantenimiento;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeUnidad;

public record RegistrarUnidadRequest(
        @NotBlank String placa,
        @NotNull TipoDeUnidad tipo,
        @Positive int pesoMaximoKg,
        @NotNull @Positive BigDecimal volumenMaximoM3,
        @PositiveOrZero int kilometraje,
        @NotNull IntervaloDeMantenimiento intervaloMantenimiento
) {
}
