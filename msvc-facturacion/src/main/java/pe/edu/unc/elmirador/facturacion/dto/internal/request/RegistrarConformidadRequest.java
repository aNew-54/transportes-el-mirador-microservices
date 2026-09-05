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
 * <p>{@code incidenciasSinResolver} va SIEMPRE, aunque este vacio: su ausencia es un error de
 * contrato y no un «sin incidencias». Por eso lleva {@code @NotNull} y no {@code @NotEmpty}.
 *
 * <p>{@code estado} va tipado como {@link EstadoDeConformidad}: un valor que no sea FIRMADA, PARCIAL
 * ni RECHAZADA es un {@code 400} en la frontera.
 */
import pe.edu.unc.elmirador.facturacion.models.vo.EstadoDeConformidad;

public record RegistrarConformidadRequest(
        @NotBlank String viajeId,
        @NotBlank String ordenDeServicioId,
        @NotNull EstadoDeConformidad estado,
        @NotNull OffsetDateTime fechaDeFirma,
        @NotNull @Valid List<ConceptoFacturableRequest> conceptosFacturables,
        @NotNull List<String> incidenciasSinResolver
) {
}
