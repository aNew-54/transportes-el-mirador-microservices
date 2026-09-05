package pe.edu.unc.elmirador.programacion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import pe.edu.unc.elmirador.programacion.models.vo.TipoDeCarga;

public record CargaRequest(
        @NotBlank String ordenDeServicioId,
        @NotNull Integer pesoKg,
        @NotNull BigDecimal volumenM3,
        @NotNull TipoDeCarga tipo,
        @NotNull Integer secuenciaDeDescarga
) {}
