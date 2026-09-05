package pe.edu.unc.elmirador.conductores.dto.response;

import java.time.LocalDate;
import java.util.List;

import pe.edu.unc.elmirador.conductores.models.vo.CategoriaDeLicencia;
import pe.edu.unc.elmirador.conductores.models.vo.SituacionDeHabilitacion;

/**
 * Legajo del conductor.
 *
 * <p>Regla 2: la entidad JPA no cruza la frontera HTTP. Este record es plano y expone los objetos de
 * valor desarmados en campos escalares, no los objetos en si.
 */
public record ConductorResponse(
        String id,
        String nombreCompleto,
        String numeroDeLicencia,
        CategoriaDeLicencia categoriaDeLicencia,
        LocalDate licenciaDesde,
        LocalDate licenciaHasta,
        SituacionDeHabilitacion situacion,
        String motivo,
        HorasResponse horas,
        List<InduccionResponse> inducciones
) {
}
