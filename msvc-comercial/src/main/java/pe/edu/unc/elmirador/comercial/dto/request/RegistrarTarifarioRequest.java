package pe.edu.unc.elmirador.comercial.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeRecargo;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeUnidad;

public record RegistrarTarifarioRequest(
        @NotNull LocalDate vigenteDesde,
        @NotNull LocalDate vigenteHasta,
        @Valid List<PrecioDeTarifarioRequest> precios,
        @Valid List<RecargoRequest> recargosEstandar
) {
    public record PrecioDeTarifarioRequest(
            @NotBlank String origen,
            @NotBlank String destino,
            @NotBlank String corredor,
            @NotNull TipoDeUnidad tipoUnidad,
            @NotNull BigDecimal precioMonto,
            @NotBlank String precioMoneda
    ) {
    }

    public record RecargoRequest(
            @NotNull TipoDeRecargo tipo,
            @NotNull BigDecimal porcentaje
    ) {
    }
}
