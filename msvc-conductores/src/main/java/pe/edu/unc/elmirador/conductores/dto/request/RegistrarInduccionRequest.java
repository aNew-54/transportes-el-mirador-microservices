package pe.edu.unc.elmirador.conductores.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Induccion de seguridad exigida por un cliente (CON-03).
 *
 * <p>{@code clienteId} es un identificador escalar de otro contexto (regla 3): el cliente vive en
 * msvc-comercial y aqui no se valida contra nada, solo se guarda.
 */
public record RegistrarInduccionRequest(

        @NotBlank
        @Size(max = 40)
        String clienteId,

        @NotNull
        LocalDate vigenteDesde,

        @NotNull
        LocalDate vigenteHasta
) {
}
