package pe.edu.unc.elmirador.unidades.mappers;

import pe.edu.unc.elmirador.unidades.dto.response.RepuestoResponse;
import pe.edu.unc.elmirador.unidades.models.entity.Repuesto;

public final class RepuestoMapper {

    private RepuestoMapper() {
    }

    public static RepuestoResponse aResponse(Repuesto repuesto) {
        return new RepuestoResponse(
                repuesto.getId(),
                repuesto.getCodigo(),
                repuesto.getDescripcion(),
                repuesto.getExistencias(),
                repuesto.getStockMinimo(),
                repuesto.getCostoUnitario().monto(),
                repuesto.getCostoUnitario().codigoMoneda(),
                repuesto.requiereReposicion()
        );
    }
}
