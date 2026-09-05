package pe.edu.unc.elmirador.conductores.dto.internal.request;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Contrato 6 · Ejecucion → Conductores. Alimenta CON-02.
 *
 * <pre>
 * { "viajeId": "VIA-2026-00045", "horas": 8.5,
 *   "desde": "2026-09-10T06:00:00-05:00", "hasta": "2026-09-10T14:30:00-05:00" }
 * </pre>
 *
 * <p>Las horas las mide Ejecucion y llegan en el cuerpo. Conductores no las recalcula a partir de la
 * ventana: son dos cosas distintas —el tramo pudo tener paradas— y recalcularlas seria reinterpretar
 * un dato del que Ejecucion es duena.
 */
public record ReportarHorasRequest(

        @NotBlank
        String viajeId,

        @NotNull @PositiveOrZero
        BigDecimal horas,

        @NotNull
        OffsetDateTime desde,

        @NotNull
        OffsetDateTime hasta
) {
}
