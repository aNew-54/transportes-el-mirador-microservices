package pe.edu.unc.elmirador.unidades.dto.response;

import java.math.BigDecimal;

public record TrabajoRealizadoResponse(
        String id,
        String descripcion,
        BigDecimal costoManoDeObra,
        String moneda,
        String repuestoId,
        Integer cantidadRepuesto
) {
}
