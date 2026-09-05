package pe.edu.unc.elmirador.comercial.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pe.edu.unc.elmirador.comercial.models.vo.ModalidadDePago;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeCarga;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeUnidad;

/**
 * Alta de una orden directa: un cliente con contrato marco pide un servicio sin cotizar.
 *
 * <p>{@code contratoId} y {@code tipoUnidad} son obligatorios porque la tarifa sale de la tarifa
 * pactada del contrato para esa ruta y ese tipo de unidad. Sin ellos no hay de donde sacar el precio,
 * y ponerlo a ojo desde el servicio seria inventarse un importe.
 */
public record CrearOrdenRequest(
        @NotBlank String clienteId,
        @NotBlank String contratoId,
        @NotNull TipoDeUnidad tipoUnidad,
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
