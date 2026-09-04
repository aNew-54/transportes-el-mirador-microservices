package pe.edu.unc.elmirador.cobranza.models.vo;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDate;

/**
 * Objeto de valor inmutable que representa el estado crediticio de un cliente.
 */
@Embeddable
public record EstadoCrediticio(
    @Enumerated(EnumType.STRING) SituacionCrediticia situacion,
    String motivo,
    LocalDate fechaDeCambio
) {
    public EstadoCrediticio {
        if (situacion == null) {
            throw new IllegalArgumentException("La situacion crediticia es obligatoria");
        }
        if (fechaDeCambio == null) {
            throw new IllegalArgumentException("La fecha de cambio es obligatoria");
        }
        if (situacion != SituacionCrediticia.VIGENTE) {
            if (motivo == null || motivo.isBlank()) {
                throw new IllegalArgumentException("El motivo es obligatorio para situacion " + situacion);
            }
            motivo = motivo.trim();
        } else {
            if (motivo != null) {
                motivo = motivo.trim();
            }
        }
    }

    public boolean permiteCredito() {
        return this.situacion == SituacionCrediticia.VIGENTE;
    }

    public static EstadoCrediticio vigente(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de cambio es obligatoria");
        }
        return new EstadoCrediticio(SituacionCrediticia.VIGENTE, null, fecha);
    }

    public static EstadoCrediticio suspendido(String motivo, LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de cambio es obligatoria");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo de suspension es obligatorio");
        }
        return new EstadoCrediticio(SituacionCrediticia.SUSPENDIDO, motivo, fecha);
    }
}
