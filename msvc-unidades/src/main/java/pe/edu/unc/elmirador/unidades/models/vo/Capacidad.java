package pe.edu.unc.elmirador.unidades.models.vo;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public record Capacidad(int pesoMaximoKg, BigDecimal volumenMaximoM3) {

    public Capacidad {
        if (pesoMaximoKg <= 0) {
            throw new IllegalArgumentException("El peso maximo debe ser positivo: " + pesoMaximoKg);
        }
        if (volumenMaximoM3 == null || volumenMaximoM3.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El volumen maximo debe ser positivo: " + volumenMaximoM3);
        }
    }

    public boolean admite(int pesoKg, BigDecimal volumenM3) {
        if (pesoKg < 0 || pesoKg > pesoMaximoKg) {
            return false;
        }
        if (volumenM3 == null || volumenM3.compareTo(BigDecimal.ZERO) < 0 || volumenM3.compareTo(volumenMaximoM3) > 0) {
            return false;
        }
        return true;
    }
}
