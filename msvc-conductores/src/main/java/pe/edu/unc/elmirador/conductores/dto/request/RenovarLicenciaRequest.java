package pe.edu.unc.elmirador.conductores.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import pe.edu.unc.elmirador.conductores.models.vo.CategoriaDeLicencia;

/** Renovacion de la licencia o cambio de categoria. Los tres campos viajan juntos: son un solo hecho. */
public record RenovarLicenciaRequest(

        @NotBlank
        @Pattern(regexp = "^[A-Za-z]\\d{8}$", message = "El formato del numero de licencia es invalido")
        String numeroDeLicencia,

        @NotNull
        CategoriaDeLicencia categoriaDeLicencia,

        @NotNull
        LocalDate vigenteDesde,

        @NotNull
        LocalDate vigenteHasta
) {
}
