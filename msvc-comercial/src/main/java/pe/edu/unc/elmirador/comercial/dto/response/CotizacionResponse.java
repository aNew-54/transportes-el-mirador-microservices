package pe.edu.unc.elmirador.comercial.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CotizacionResponse(
        String id,
        String clienteId,
        String tarifarioId,
        int cargaPesoKg,
        BigDecimal cargaVolumenM3,
        String cargaTipo,
        String rutaOrigen,
        String rutaDestino,
        String rutaCorredor,
        TarifaResponse tarifa,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        String estado,
        String motivoDeRechazo
) {
}
