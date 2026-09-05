package pe.edu.unc.elmirador.comercial.dto.internal.request;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import pe.edu.unc.elmirador.comercial.models.vo.DecisionDeDiferencia;

/**
 * Contrato 7 · Ejecucion → Comercial. Permite el reajuste de tarifa antes de facturar (ORD-01).
 *
 * <p>{@code importeDelReajuste} va nulo cuando la {@code decision} es {@code RECHAZADA}. En los otros
 * dos casos es lo que permite aplicar ORD-01: una orden ya programada no admite cambio de carga sin
 * generar reajuste, y el agregado se niega si falta el importe.
 */
public record DiferenciaDeCargaRequest(

        @NotBlank
        String viajeId,

        @NotNull @Valid
        CargaInfo declarado,

        @NotNull @Valid
        CargaInfo real,

        @NotNull
        DecisionDeDiferencia decision,

        @Valid
        ImporteRequest importeDelReajuste,

        @NotNull
        OffsetDateTime momento
) {

    public record CargaInfo(
            @Positive int pesoKg,
            @NotNull BigDecimal volumenM3,
            String embalaje
    ) {
    }
}
