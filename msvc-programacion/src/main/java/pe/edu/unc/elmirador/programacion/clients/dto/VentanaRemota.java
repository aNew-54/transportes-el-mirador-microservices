package pe.edu.unc.elmirador.programacion.clients.dto;

import java.time.OffsetDateTime;

public record VentanaRemota(
    OffsetDateTime inicio,
    OffsetDateTime fin
) {}
