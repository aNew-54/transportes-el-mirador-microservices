package pe.edu.unc.elmirador.programacion.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RutaRequest(
        @NotBlank String origen,
        @NotBlank String destino,
        @NotBlank String corredor
) {}
