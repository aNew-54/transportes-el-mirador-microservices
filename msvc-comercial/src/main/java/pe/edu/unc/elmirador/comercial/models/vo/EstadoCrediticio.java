package pe.edu.unc.elmirador.comercial.models.vo;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDate;

/**
 * Objeto de valor inmutable que representa una copia local del estado crediticio con marca de tiempo.
 * Sostiene CLI-01 y ORD-02. No es la fuente de verdad.
 */
@Embeddable
public record EstadoCrediticio(
    @Enumerated(EnumType.STRING)
    SituacionCrediticia situacion,
    LocalDate fechaDeCambio
) {

    public EstadoCrediticio {
        if (situacion == null) {
            throw new IllegalArgumentException("La situacion crediticia es obligatoria");
        }
        if (fechaDeCambio == null) {
            throw new IllegalArgumentException("La fecha de cambio es obligatoria");
        }
    }

    public static EstadoCrediticio vigente(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de cambio es obligatoria");
        }
        return new EstadoCrediticio(SituacionCrediticia.VIGENTE, fecha);
    }

    public static EstadoCrediticio suspendido(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de cambio es obligatoria");
        }
        return new EstadoCrediticio(SituacionCrediticia.SUSPENDIDO, fecha);
    }

    public boolean permiteCredito() {
        return this.situacion == SituacionCrediticia.VIGENTE;
    }
}
