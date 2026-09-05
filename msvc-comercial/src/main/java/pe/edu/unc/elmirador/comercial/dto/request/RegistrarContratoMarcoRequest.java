package pe.edu.unc.elmirador.comercial.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeUnidad;

public record RegistrarContratoMarcoRequest(
        @NotBlank String clienteId,
        @NotNull LocalDate vigenteDesde,
        @NotNull LocalDate vigenteHasta,
        @Min(0) int tiempoLibreHoras,
        @NotNull boolean consolidacionPermitida,
        List<String> consolidacionRestricciones,
        @Valid List<TarifaPactadaRequest> tarifasPactadas
) {
    public record TarifaPactadaRequest(
            @NotBlank String rutaOrigen,
            @NotBlank String rutaDestino,
            @NotBlank String rutaCorredor,
            @NotNull TipoDeUnidad tipoUnidad,
            @NotNull BigDecimal precioMonto,
            @NotBlank String precioMoneda
    ) {
    }
}
