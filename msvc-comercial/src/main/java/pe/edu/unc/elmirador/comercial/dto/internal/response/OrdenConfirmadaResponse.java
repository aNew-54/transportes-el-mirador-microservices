package pe.edu.unc.elmirador.comercial.dto.internal.response;

import java.util.List;

/**
 * Contrato 1 · Programacion → Comercial. Sostiene VIA-04.
 *
 * <p>{@code permiteConsolidacion} y {@code restriccionesConsolidacion} salen de la clausula del
 * contrato marco, ya resuelta por Comercial: Programacion consume la decision y no reinterpreta el
 * contrato marco.
 */
public record OrdenConfirmadaResponse(
        String ordenId,
        String clienteId,
        String estado,
        CargaResponse carga,
        RutaResponse ruta,
        VentanaResponse ventana,
        boolean permiteConsolidacion,
        List<String> restriccionesConsolidacion,
        String tipoUnidadRequerido
) {
    public record CargaResponse(
            int pesoKg,
            java.math.BigDecimal volumenM3,
            String embalaje,
            String naturaleza
    ) {}

    public record RutaResponse(
            String origen,
            String destino,
            String corredor,
            Integer distanciaKm
    ) {}

    /** Instantes ISO 8601 con offset, como pide la regla 2 de los contratos. */
    public record VentanaResponse(
            java.time.OffsetDateTime inicio,
            java.time.OffsetDateTime fin
    ) {}
}
