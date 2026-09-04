package pe.edu.unc.elmirador.conductores.models.vo;

import jakarta.persistence.Embeddable;
import pe.edu.unc.elmirador.conductores.exceptions.HorasExcedidasException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Embeddable
public record HorasDeConduccion(
        BigDecimal horas,
        PeriodoDeVigencia ventanaDeComputo
) {

    public static final BigDecimal MAXIMO_HORAS = new BigDecimal("10.00");

    public HorasDeConduccion {
        if (horas == null) {
            throw new IllegalArgumentException("Las horas no pueden ser nulas");
        }
        if (ventanaDeComputo == null) {
            throw new IllegalArgumentException("La ventana de computo no puede ser nula");
        }
        if (horas.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Las horas no pueden ser negativas");
        }
        if (horas.compareTo(MAXIMO_HORAS) > 0) {
            throw new HorasExcedidasException(
                    "Las horas acumuladas (" + horas + ") superan el maximo normado de " + MAXIMO_HORAS
            );
        }
        horas = horas.setScale(2, RoundingMode.HALF_UP);
    }

    public static HorasDeConduccion cero(PeriodoDeVigencia ventanaDeComputo) {
        return new HorasDeConduccion(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), ventanaDeComputo);
    }

    public boolean tieneDisponibles(BigDecimal requeridas) {
        if (requeridas == null) {
            throw new IllegalArgumentException("Las horas requeridas no pueden ser nulas");
        }
        if (requeridas.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Las horas requeridas no pueden ser negativas");
        }
        return this.horas.add(requeridas).compareTo(MAXIMO_HORAS) <= 0;
    }

    public BigDecimal disponibles() {
        BigDecimal disp = MAXIMO_HORAS.subtract(this.horas);
        if (disp.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return disp.setScale(2, RoundingMode.HALF_UP);
    }

    public HorasDeConduccion acumular(BigDecimal adicionales) {
        if (adicionales == null) {
            throw new IllegalArgumentException("Las horas a acumular no pueden ser nulas");
        }
        if (adicionales.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Las horas a acumular no pueden ser negativas");
        }
        BigDecimal nuevoTotal = this.horas.add(adicionales).setScale(2, RoundingMode.HALF_UP);
        if (nuevoTotal.compareTo(MAXIMO_HORAS) > 0) {
            throw new HorasExcedidasException(
                    "Las horas acumuladas (" + nuevoTotal + ") superarian el maximo normado de " + MAXIMO_HORAS
            );
        }
        return new HorasDeConduccion(nuevoTotal, this.ventanaDeComputo);
    }
}
