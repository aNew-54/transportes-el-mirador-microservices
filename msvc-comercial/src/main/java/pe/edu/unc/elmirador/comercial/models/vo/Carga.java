package pe.edu.unc.elmirador.comercial.models.vo;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/**
 * Objeto de valor inmutable que representa los datos cuantitativos y cualitativos de la carga.
 */
@Embeddable
public record Carga(int pesoKg, BigDecimal volumenM3, TipoDeCarga tipo) {

    public Carga {
        if (pesoKg <= 0) {
            throw new IllegalArgumentException("El peso de la carga debe ser mayor a cero: " + pesoKg);
        }
        if (volumenM3 == null) {
            throw new IllegalArgumentException("El volumen de la carga es obligatorio");
        }
        if (volumenM3.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El volumen de la carga debe ser mayor a cero: " + volumenM3);
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de carga es obligatorio");
        }
    }
}
