package pe.edu.unc.elmirador.conductores.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.edu.unc.elmirador.conductores.models.vo.CategoriaDeLicencia;

/**
 * Alta de un conductor.
 *
 * <p>El {@code @Pattern} no sustituye a {@code NumeroDeLicencia}: el objeto de valor sigue rechazando
 * el formato malo, y lo seguira haciendo cuando el dato llegue por otra via. La anotacion solo
 * adelanta el {@code 400} para que el error se lea como un fallo de validacion y no de dominio.
 */
public record RegistrarConductorRequest(

        @NotBlank
        @Size(max = 200)
        String nombreCompleto,

        @NotBlank
        @Pattern(regexp = "^[A-Za-z]\\d{8}$", message = "El formato del numero de licencia es invalido")
        String numeroDeLicencia,

        @NotNull
        CategoriaDeLicencia categoriaDeLicencia,

        @NotNull
        LocalDate licenciaDesde,

        @NotNull
        LocalDate licenciaHasta
) {
}
