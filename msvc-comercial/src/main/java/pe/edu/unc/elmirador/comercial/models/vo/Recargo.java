package pe.edu.unc.elmirador.comercial.models.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;

/**
 * Objeto de valor inmutable que representa un recargo porcentual aplicable a una tarifa base.
 * El porcentaje debe ubicarse en el intervalo (0, 100].
 */
@Embeddable
public record Recargo(
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 30, nullable = false)
    TipoDeRecargo tipo,

    @Column(name = "porcentaje", precision = 5, scale = 2, nullable = false)
    BigDecimal porcentaje
) {

    public Recargo {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de recargo es obligatorio");
        }
        if (porcentaje == null) {
            throw new IllegalArgumentException("El porcentaje de recargo es obligatorio");
        }
        if (porcentaje.compareTo(BigDecimal.ZERO) <= 0 || porcentaje.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException(
                "El porcentaje de recargo debe ubicarse en el rango (0, 100]: " + porcentaje
            );
        }
    }
}
