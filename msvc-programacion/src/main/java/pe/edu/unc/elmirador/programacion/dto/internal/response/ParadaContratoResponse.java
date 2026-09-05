package pe.edu.unc.elmirador.programacion.dto.internal.response;

import java.time.OffsetDateTime;

public record ParadaContratoResponse(
        int secuencia,
        String tipo,
        String ordenDeServicioId,
        UbicacionContratoResponse ubicacion,
        OffsetDateTime horaEstimada) {
}
