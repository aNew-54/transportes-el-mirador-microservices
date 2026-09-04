package pe.edu.unc.elmirador.programacion.models.vo;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public record Capacidad(int pesoMaximoKg, BigDecimal volumenMaximoM3) {

    public Capacidad {
        if (pesoMaximoKg <= 0) {
            throw new IllegalArgumentException("El peso maximo debe ser positivo: " + pesoMaximoKg);
        }
        if (volumenMaximoM3 == null) {
            throw new IllegalArgumentException("El volumen maximo es obligatorio");
        }
        if (volumenMaximoM3.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El volumen maximo debe ser positivo: " + volumenMaximoM3);
        }
    }
}
