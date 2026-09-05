package pe.edu.unc.elmirador.ejecucion.clients.dto;

import java.time.OffsetDateTime;

public record ParadaRemota(
        int secuencia,
        String tipo,
        String ordenDeServicioId,
        UbicacionRemota ubicacion,
        OffsetDateTime horaEstimada
) {
}
