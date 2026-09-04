package pe.edu.unc.elmirador.programacion.models.vo;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

@Embeddable
public record Carga(
        String ordenDeServicioId,
        int pesoKg,
        BigDecimal volumenM3,
        TipoDeCarga tipo,
        int secuenciaDeDescarga) {

    public Carga {
        if (ordenDeServicioId == null || ordenDeServicioId.isBlank()) {
            throw new IllegalArgumentException("El ordenDeServicioId es obligatorio");
        }
        if (pesoKg <= 0) {
            throw new IllegalArgumentException("El peso debe ser positivo: " + pesoKg);
        }
        if (volumenM3 == null) {
            throw new IllegalArgumentException("El volumen es obligatorio");
        }
        if (volumenM3.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El volumen debe ser positivo: " + volumenM3);
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de carga es obligatorio");
        }
        if (secuenciaDeDescarga <= 0) {
            throw new IllegalArgumentException("La secuencia de descarga debe ser mayor a cero: " + secuenciaDeDescarga);
        }
        ordenDeServicioId = ordenDeServicioId.trim();
    }

    public boolean esCompatibleCon(Carga otra) {
        if (otra == null) {
            throw new IllegalArgumentException("La otra carga es obligatoria");
        }
        // TODO S1b: VIA-05 - compatibilidad fisica por pares segun matriz de compatibilidad
        return true;
    }
}
