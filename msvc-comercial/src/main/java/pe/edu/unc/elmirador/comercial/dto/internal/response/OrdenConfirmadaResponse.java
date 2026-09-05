package pe.edu.unc.elmirador.comercial.dto.internal.response;

import java.util.List;

/**
 * Contrato 1: Orden Confirmada
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

    public record VentanaResponse(
            String inicio,
            String fin
    ) {}
}
