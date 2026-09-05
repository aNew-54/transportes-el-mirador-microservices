package pe.edu.unc.elmirador.comercial.models.vo;

import java.time.OffsetDateTime;

import jakarta.persistence.Embeddable;

/**
 * Franja en la que el cliente pide el servicio. La publica el contrato 1 para que Programación sepa
 * si una orden cabe en un viaje: VIA-03 exige ventanas compatibles entre las órdenes consolidadas.
 *
 * <p>Sin esto, la orden no decía cuándo se quiere el servicio y Programación tenía que suponerlo.
 */
@Embeddable
public record VentanaDeServicio(OffsetDateTime inicio, OffsetDateTime fin) {

    public VentanaDeServicio {
        if (inicio == null || fin == null) {
            throw new IllegalArgumentException("El inicio y el fin de la ventana son obligatorios");
        }
        if (!fin.isAfter(inicio)) {
            throw new IllegalArgumentException("El fin de la ventana debe ser posterior al inicio");
        }
    }
}
