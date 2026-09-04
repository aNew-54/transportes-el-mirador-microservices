package pe.edu.unc.elmirador.comercial.models.vo;

import jakarta.persistence.Embeddable;

/**
 * Objeto de valor inmutable que representa la ruta de transporte normalizada a mayusculas.
 */
@Embeddable
public record Ruta(String origen, String destino, String corredor) {

    public Ruta {
        if (origen == null || origen.isBlank()) {
            throw new IllegalArgumentException("El origen de la ruta es obligatorio");
        }
        if (destino == null || destino.isBlank()) {
            throw new IllegalArgumentException("El destino de la ruta es obligatorio");
        }
        if (corredor == null || corredor.isBlank()) {
            throw new IllegalArgumentException("El corredor vial es obligatorio");
        }
        origen = origen.trim().toUpperCase();
        destino = destino.trim().toUpperCase();
        corredor = corredor.trim().toUpperCase();
    }
}
