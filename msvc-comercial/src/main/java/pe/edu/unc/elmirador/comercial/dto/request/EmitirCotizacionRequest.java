package pe.edu.unc.elmirador.comercial.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeCarga;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeUnidad;

public record EmitirCotizacionRequest(
        @NotBlank String clienteId,
        @NotNull int cargaPesoKg,
        @NotNull BigDecimal cargaVolumenM3,
        @NotNull TipoDeCarga cargaTipo,
        @NotBlank String rutaOrigen,
        @NotBlank String rutaDestino,
        @NotBlank String rutaCorredor,
        @NotNull TipoDeUnidad tipoUnidadRequerida,
        BigDecimal descuentoPorcentaje,
        String descuentoAutorizadoPor
) {
}
