package pe.edu.unc.elmirador.ejecucion.clients.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record HojaDeRutaRemota(
        String viajeId,
        String estado,
        String unidadId,
        List<String> conductorIds,
        String observaciones,
        List<ParadaRemota> paradas
) {
}
