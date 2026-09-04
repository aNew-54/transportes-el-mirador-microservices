package pe.edu.unc.elmirador.conductores.models.vo;

import jakarta.persistence.Embeddable;
import pe.edu.unc.elmirador.conductores.exceptions.HorasExcedidasException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

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

    /**
     * Indica si la ventana de computo vigente cubre esa fecha.
     *
     * <p>El maximo normado es de diez horas en veinticuatro (DS 017-2009-MTC). El dominio trabaja
     * en dias calendario, asi que la ventana es exactamente un dia: se compara contra {@code desde}
     * y no con {@link PeriodoDeVigencia#estaVigenteEn}, que es inclusivo en los dos extremos y
     * convertiria una ventana de veinticuatro horas en una de cuarenta y ocho. Un conductor que
     * agoto sus horas el lunes debe volver a tenerlas el martes.
     */
    public boolean cubre(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha no puede ser nula");
        }
        return ventanaDeComputo.desde().isEqual(fecha);
    }

    /** Abre la ventana de computo del dia indicado, con el acumulado en cero. */
    public static HorasDeConduccion ventanaDe(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha no puede ser nula");
        }
        return cero(new PeriodoDeVigencia(fecha, fecha.plusDays(1)));
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
        // El constructor ya impide horas > MAXIMO_HORAS, asi que la resta nunca es negativa.
        return MAXIMO_HORAS.subtract(this.horas).setScale(2, RoundingMode.HALF_UP);
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
