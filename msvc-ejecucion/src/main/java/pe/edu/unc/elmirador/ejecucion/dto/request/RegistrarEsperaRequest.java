package pe.edu.unc.elmirador.ejecucion.dto.request;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** La espera de una parada. El excedente no viene: lo calcula el VO con estos tres datos. */
public record RegistrarEsperaRequest(
        @NotNull OffsetDateTime inicio,
        @NotNull OffsetDateTime fin,
        @NotNull @PositiveOrZero Integer tiempoLibreHoras
) {
}
