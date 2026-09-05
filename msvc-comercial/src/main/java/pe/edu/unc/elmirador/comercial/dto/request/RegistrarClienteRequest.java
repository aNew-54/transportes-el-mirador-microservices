package pe.edu.unc.elmirador.comercial.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.edu.unc.elmirador.comercial.models.vo.ModalidadDePago;

public record RegistrarClienteRequest(
        @NotBlank @Pattern(regexp = "^(10|15|17|20)\\d{9}$") String ruc,
        @NotBlank @Size(max = 200) String razonSocial,
        @NotNull ModalidadDePago modalidadDePago,
        @Min(0) int plazoEnDias
) {
}
