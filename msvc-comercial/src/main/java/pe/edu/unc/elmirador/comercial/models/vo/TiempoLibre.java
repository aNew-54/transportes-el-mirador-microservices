package pe.edu.unc.elmirador.comercial.models.vo;

import jakarta.persistence.Embeddable;

/**
 * Objeto de valor inmutable que representa las horas de tiempo libre de espera pactadas.
 */
@Embeddable
public record TiempoLibre(int horas) {

    public TiempoLibre {
        if (horas < 0) {
            throw new IllegalArgumentException("Las horas de tiempo libre no pueden ser negativas: " + horas);
        }
    }
}
