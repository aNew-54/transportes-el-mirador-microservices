package pe.edu.unc.elmirador.ejecucion.dto.request;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import pe.edu.unc.elmirador.ejecucion.models.vo.ConceptoDeGasto;

public record RendirGastoRequest(
        @NotNull ConceptoDeGasto concepto,
        @NotNull @PositiveOrZero BigDecimal importeMonto,
        @NotBlank String importeMoneda,
        @NotBlank String comprobanteTipo,
        @NotBlank String comprobanteNumero,
        @NotNull OffsetDateTime comprobanteFecha,
        @NotBlank String descripcion
) {
}
