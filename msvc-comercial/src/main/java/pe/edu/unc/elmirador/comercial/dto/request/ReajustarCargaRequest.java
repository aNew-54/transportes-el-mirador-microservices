package pe.edu.unc.elmirador.comercial.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeCarga;

public record ReajustarCargaRequest(
        @NotNull int cargaPesoKg,
        @NotNull BigDecimal cargaVolumenM3,
        @NotNull TipoDeCarga cargaTipo,
        BigDecimal reajusteMonto,
        String reajusteMoneda
) {
}
