package pe.edu.unc.elmirador.comercial.models.vo;

import jakarta.persistence.Embeddable;
import pe.edu.unc.elmirador.comercial.exceptions.RucInvalidoException;

/**
 * Objeto de valor que representa un RUC valido de 11 digitos comenzando en 10, 15, 17 o 20.
 */
@Embeddable
public record Ruc(String valor) {

    public Ruc {
        if (valor == null || valor.isBlank()) {
            throw new RucInvalidoException("El RUC es obligatorio y no puede estar vacio");
        }
        valor = valor.trim();
        if (!valor.matches("^(10|15|17|20)\\d{9}$")) {
            throw new RucInvalidoException(
                "El RUC debe tener 11 digitos numericos y comenzar con 10, 15, 17 o 20: " + valor
            );
        }
    }
}
