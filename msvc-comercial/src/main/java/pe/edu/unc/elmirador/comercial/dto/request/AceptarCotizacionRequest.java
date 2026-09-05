package pe.edu.unc.elmirador.comercial.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import pe.edu.unc.elmirador.comercial.models.vo.ModalidadDePago;

public record AceptarCotizacionRequest(
        @NotNull ModalidadDePago modalidadDePago,
        @Min(0) int plazoEnDias
) {
}
