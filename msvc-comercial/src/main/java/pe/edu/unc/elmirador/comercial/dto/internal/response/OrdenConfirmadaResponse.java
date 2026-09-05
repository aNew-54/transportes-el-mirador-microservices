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
    /**
     * {@code tipo} es el que decide VIA-05, la compatibilidad fisica entre dos cargas que compartirian
     * plataforma. Sin el, Programacion tenia que adivinarlo a partir de {@code embalaje} y
     * {@code naturaleza}, y su regla de reserva devolvia {@code GENERAL} para todo lo que no
     * reconociera: una maquinaria pesada mal clasificada consolida con cualquier cosa.
     *
     * <p>Los dos contextos tienen el mismo enumerado de tres valores, asi que viaja tal cual.
     * {@code embalaje} y {@code naturaleza} se quedan porque describen la carga para el conductor y
     * para el seguro, no para la decision de estiba.
     */
    public record CargaResponse(
            int pesoKg,
            java.math.BigDecimal volumenM3,
            String tipo,
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
