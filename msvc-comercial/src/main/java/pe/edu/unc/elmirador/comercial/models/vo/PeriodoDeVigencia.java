package pe.edu.unc.elmirador.comercial.models.vo;

import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Objeto de valor inmutable que modela un periodo de vigencia temporal con extremos inclusivos [desde, hasta].
 */
@Embeddable
public record PeriodoDeVigencia(LocalDate desde, LocalDate hasta) {

    public PeriodoDeVigencia {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin del periodo son obligatorias");
        }
        if (hasta.isBefore(desde)) {
            throw new IllegalArgumentException(
                "La fecha 'hasta' (" + hasta + ") no puede ser anterior a 'desde' (" + desde + ")"
            );
        }
    }

    public static PeriodoDeVigencia de(LocalDate desde, LocalDate hasta) {
        return new PeriodoDeVigencia(desde, hasta);
    }

    public static PeriodoDeVigencia de(LocalDate desde, int dias) {
        if (desde == null) {
            throw new IllegalArgumentException("La fecha de inicio es obligatoria");
        }
        if (dias <= 0) {
            throw new IllegalArgumentException("El numero de dias de vigencia debe ser mayor a cero: " + dias);
        }
        return new PeriodoDeVigencia(desde, desde.plusDays(dias - 1));
    }

    public boolean estaVigenteEn(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de consulta es obligatoria");
        }
        return !fecha.isBefore(desde) && !fecha.isAfter(hasta);
    }

    public boolean seSolapaCon(PeriodoDeVigencia otro) {
        if (otro == null) {
            throw new IllegalArgumentException("El periodo de vigencia a comparar es obligatorio");
        }
        return !this.desde.isAfter(otro.hasta) && !this.hasta.isBefore(otro.desde);
    }

    public int diasDeVigencia() {
        return (int) ChronoUnit.DAYS.between(desde, hasta) + 1;
    }
}
