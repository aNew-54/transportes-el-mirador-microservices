package pe.edu.unc.elmirador.unidades.dto.response;

import java.math.BigDecimal;

public record RepuestoResponse(
        String id,
        String codigo,
        String descripcion,
        int existencias,
        int stockMinimo,
        BigDecimal costoUnitario,
        String moneda,
        boolean requiereReposicion
) {
}
