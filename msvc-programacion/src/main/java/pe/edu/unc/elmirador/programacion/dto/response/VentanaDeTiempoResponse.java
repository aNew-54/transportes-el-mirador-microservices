package pe.edu.unc.elmirador.programacion.dto.response;

import java.time.OffsetDateTime;

public record VentanaDeTiempoResponse(
        OffsetDateTime desde,
        OffsetDateTime hasta
) {}
