package pe.edu.unc.elmirador.comercial.dto.response;

import java.math.BigDecimal;

public record OrdenDeServicioResponse(
        String id,
        String clienteId,
        String contratoId,
        int cargaPesoKg,
        BigDecimal cargaVolumenM3,
        String cargaTipo,
        String rutaOrigen,
        String rutaDestino,
        String rutaCorredor,
        TarifaResponse tarifa,
        CondicionDePagoResponse condicionDePago,
        String estado,
        TarifaResponse falsoFlete,
        String canceladoPor
) {
}
