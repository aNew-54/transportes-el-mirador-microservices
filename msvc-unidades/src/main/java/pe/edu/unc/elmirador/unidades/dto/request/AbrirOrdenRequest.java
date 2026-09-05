package pe.edu.unc.elmirador.unidades.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeMantenimiento;

public record AbrirOrdenRequest(
        @NotBlank String unidadId,
        @NotNull TipoDeMantenimiento tipoMantenimiento,
        @PositiveOrZero int kilometrajeAtencion,
        @NotBlank String moneda
) {
}
