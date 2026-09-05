package pe.edu.unc.elmirador.programacion.dto.response;

import java.util.List;

public record AsignacionDeRecursosResponse(
        String unidadId,
        List<String> conductorIds,
        Boolean conRelevo
) {}
