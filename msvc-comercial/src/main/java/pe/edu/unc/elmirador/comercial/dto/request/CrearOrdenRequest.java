package pe.edu.unc.elmirador.comercial.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pe.edu.unc.elmirador.comercial.models.vo.ModalidadDePago;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeCarga;

public record CrearOrdenRequest(
        @NotBlank String clienteId,
        String contratoId,
        @NotNull int cargaPesoKg,
        @NotNull BigDecimal cargaVolumenM3,
        @NotNull TipoDeCarga cargaTipo,
        @NotBlank String rutaOrigen,
        @NotBlank String rutaDestino,
        @NotBlank String rutaCorredor,
        @NotNull ModalidadDePago modalidadDePago,
        @Min(0) int plazoEnDias
) {
}
