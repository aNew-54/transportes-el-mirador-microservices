package pe.edu.unc.elmirador.programacion.dto.response;

import java.time.OffsetDateTime;

public record ParadaResponse(
        Integer secuencia,
        String tipo,
        String ordenDeServicioId,
        String ubicacion,
        OffsetDateTime horaEstimada
) {}
