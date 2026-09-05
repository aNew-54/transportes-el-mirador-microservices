package pe.edu.unc.elmirador.programacion.dto.response;

import java.time.OffsetDateTime;

public record ParadaResponse(
        Integer secuencia,
        String tipo,
        String ordenDeServicioId,
        String ubicacionDireccion,
        String ubicacionDistrito,
        String ubicacionReferencia,
        String ubicacionContacto,
        OffsetDateTime horaEstimada
) {}
