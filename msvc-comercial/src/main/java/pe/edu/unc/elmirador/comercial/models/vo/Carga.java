package pe.edu.unc.elmirador.comercial.models.vo;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;

/**
 * Objeto de valor inmutable que representa los datos cuantitativos y cualitativos de la carga.
 *
 * <p>{@code embalaje} —PALLETS, SACOS— y {@code naturaleza} —ALIMENTARIA, PELIGROSA— los pide el
 * contrato 1 y los compara el contrato 7 entre lo declarado y lo real. Son texto y no enumerados
 * porque los contratos sólo muestran ejemplos y no enumeran sus valores: fijar aquí el catálogo
 * completo seria codificar una regla que el diseno no ha tomado. Los dos son opcionales.
 */
@Embeddable
public record Carga(
    int pesoKg,
    BigDecimal volumenM3,
    @Enumerated(EnumType.STRING)
    TipoDeCarga tipo,
    String embalaje,
    String naturaleza
) {

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
        embalaje = normalizar(embalaje);
        naturaleza = normalizar(naturaleza);
    }

    /** Carga sin embalaje ni naturaleza declarados: los dos son opcionales. */
    public Carga(int pesoKg, BigDecimal volumenM3, TipoDeCarga tipo) {
        this(pesoKg, volumenM3, tipo, null, null);
    }

    private static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio.toUpperCase();
    }
}
