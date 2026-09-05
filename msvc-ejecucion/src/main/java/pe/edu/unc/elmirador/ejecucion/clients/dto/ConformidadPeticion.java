package pe.edu.unc.elmirador.ejecucion.clients.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ConformidadPeticion(
        String viajeId,
        String ordenDeServicioId,
        String estado,
        OffsetDateTime fechaDeFirma,
        List<ConceptoFacturableRemoto> conceptosFacturables,
        List<String> incidenciasSinResolver
) {
}
