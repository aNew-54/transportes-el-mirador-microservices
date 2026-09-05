package pe.edu.unc.elmirador.conductores.dto.internal.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Contrato 6 · Ejecucion → Conductores.
 *
 * <pre>
 * { "viajeId": "VIA-2026-00045", "tipo": "DOCUMENTARIA",
 *   "descripcion": "Retencion SUTRAN por guia incompleta.", "atribuible": true }
 * </pre>
 *
 * <p>{@code atribuible} es {@code Boolean} y no {@code boolean}: con el primitivo, un cuerpo que omita
 * el campo llegaria como {@code false} y la incidencia quedaria registrada como no atribuible sin que
 * nadie lo haya dicho. Con el objeto, {@code @NotNull} lo convierte en el {@code 400} que es.
 */
public record ReportarIncidenciaRequest(

        @NotBlank
        String viajeId,

        @NotBlank
        @Size(max = 40)
        String tipo,

        @NotBlank
        @Size(max = 500)
        String descripcion,

        @NotNull
        Boolean atribuible
) {
}
