package pe.edu.unc.elmirador.comercial.dto.internal.request;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DiferenciaDeCargaRequest(
        @NotBlank String viajeId,
        @NotNull CargaInfo declarado,
        @NotNull CargaInfo real,
        @NotBlank String decision,
        @NotNull OffsetDateTime momento
) {
    public record CargaInfo(
            int pesoKg,
            BigDecimal volumenM3,
            String embalaje
    ) {}
}
