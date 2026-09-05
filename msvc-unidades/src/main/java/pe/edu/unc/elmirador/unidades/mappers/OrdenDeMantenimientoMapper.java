package pe.edu.unc.elmirador.unidades.mappers;

import java.util.List;

import pe.edu.unc.elmirador.unidades.dto.response.OrdenDeMantenimientoResponse;
import pe.edu.unc.elmirador.unidades.dto.response.TrabajoRealizadoResponse;
import pe.edu.unc.elmirador.unidades.models.entity.OrdenDeMantenimiento;
import pe.edu.unc.elmirador.unidades.models.entity.TrabajoRealizado;

public final class OrdenDeMantenimientoMapper {

    private OrdenDeMantenimientoMapper() {
    }

    public static OrdenDeMantenimientoResponse aResponse(OrdenDeMantenimiento orden) {
        List<TrabajoRealizadoResponse> trabajos = orden.getTrabajos().stream()
                .map(OrdenDeMantenimientoMapper::aResponse)
                .toList();

        return new OrdenDeMantenimientoResponse(
                orden.getId(),
                orden.getUnidadId(),
                orden.getTipoMantenimiento(),
                orden.getKmAtencion().valor(),
                orden.getEstado(),
                orden.getFechaApertura(),
                orden.getFechaCierre(),
                orden.costoTotal().monto(),
                orden.costoTotal().codigoMoneda(),
                trabajos
        );
    }

    private static TrabajoRealizadoResponse aResponse(TrabajoRealizado trabajo) {
        return new TrabajoRealizadoResponse(
                trabajo.getId(),
                trabajo.getDescripcion(),
                trabajo.getCostoManoDeObra().monto(),
                trabajo.getCostoManoDeObra().codigoMoneda(),
                trabajo.getRepuestoId(),
                trabajo.getCantidad()
        );
    }
}
