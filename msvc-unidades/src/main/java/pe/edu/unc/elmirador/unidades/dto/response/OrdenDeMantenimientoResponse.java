package pe.edu.unc.elmirador.unidades.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import pe.edu.unc.elmirador.unidades.models.vo.EstadoDeOrden;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeMantenimiento;

public record OrdenDeMantenimientoResponse(
        String id,
        String unidadId,
        TipoDeMantenimiento tipoMantenimiento,
        int kilometrajeAtencion,
        EstadoDeOrden estado,
        LocalDate fechaApertura,
        LocalDate fechaCierre,
        BigDecimal costoTotal,
        String moneda,
        List<TrabajoRealizadoResponse> trabajos
) {
}
