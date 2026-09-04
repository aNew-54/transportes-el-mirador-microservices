package pe.edu.unc.elmirador.comercial.models.vo;

import jakarta.persistence.Embeddable;

/**
 * Objeto de valor que representa la razon social de un cliente.
 */
@Embeddable
public record RazonSocial(String valor) {

    public RazonSocial {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("La razon social es obligatoria y no puede estar vacia");
        }
        valor = valor.trim();
        if (valor.length() > 200) {
            throw new IllegalArgumentException(
                "La razon social no puede superar los 200 caracteres: " + valor.length()
            );
        }
    }
}
