package pe.edu.unc.elmirador.facturacion.dto.internal.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Contrato 8 · Ejecucion → Facturacion.
 *
 * <pre>
 * { "viajeId": "VIA-2026-00045", "ordenDeServicioId": "ORD-2026-000123", "estado": "FIRMADA",
 *   "fechaDeFirma": "2026-09-10T15:20:00-05:00",
 *   "conceptosFacturables": [ { "concepto": "ESTIBA", "monto": "180.00", "moneda": "PEN" } ],
 *   "incidenciasSinResolver": [] }
 * </pre>
 *
 * {@code incidenciasSinResolver} va siempre, la ausencia es 400.
 */
public record RegistrarConformidadRequest(
        @NotBlank String viajeId,
        @NotBlank String ordenDeServicioId,
        @NotBlank String estado,
        @NotNull OffsetDateTime fechaDeFirma,
        @NotNull @Valid List<ConceptoFacturableRequest> conceptosFacturables,
        @NotNull List<String> incidenciasSinResolver
) {
}
