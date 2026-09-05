package pe.edu.unc.elmirador.ejecucion.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoDeEjecucion;

public record EjecucionDeViajeResponse(
        String viajeId,
        String unidadEjecutoraId,
        List<String> conductorIds,
        Integer kilometrajeFinal,
        EstadoDeEjecucion estado,
        CheckListResponse checkList,
        List<ParadaResponse> paradas,
        List<HitoResponse> hitos,
        List<IncidenciaResponse> incidencias,
        List<String> unidadesAnteriores
) {
}
