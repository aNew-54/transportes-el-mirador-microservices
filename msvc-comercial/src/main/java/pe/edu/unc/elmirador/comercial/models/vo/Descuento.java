package pe.edu.unc.elmirador.comercial.models.vo;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import pe.edu.unc.elmirador.comercial.exceptions.DescuentoNoAutorizadoException;

/**
 * Objeto de valor inmutable que representa un descuento comercial.
 * Sostiene la invariante COT-02: porcentaje entre 5% y 15% inclusive, y autorizacion de gerencia registrada obligatoria.
 */
@Embeddable
public record Descuento(BigDecimal porcentaje, String autorizadoPor) {

    public Descuento {
        if (porcentaje == null) {
            throw new IllegalArgumentException("El porcentaje de descuento es obligatorio");
        }
        if (porcentaje.compareTo(new BigDecimal("5")) < 0 || porcentaje.compareTo(new BigDecimal("15")) > 0) {
            throw new DescuentoNoAutorizadoException(
                "El porcentaje de descuento debe estar entre 5% y 15% inclusive: " + porcentaje
            );
        }
        if (autorizadoPor == null || autorizadoPor.isBlank()) {
            throw new DescuentoNoAutorizadoException(
                "El descuento exige la autorizacion de gerencia registrada (autorizadoPor no puede ser nulo ni vacio)"
            );
        }
        autorizadoPor = autorizadoPor.trim();
    }
}
