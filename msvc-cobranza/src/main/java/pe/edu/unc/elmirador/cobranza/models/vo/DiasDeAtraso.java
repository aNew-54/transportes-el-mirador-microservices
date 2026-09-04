package pe.edu.unc.elmirador.cobranza.models.vo;

import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Objeto de valor inmutable que representa los dias de atraso respecto a la fecha de vencimiento.
 * Negativo indica que el documento aun no vence.
 */
@Embeddable
public record DiasDeAtraso(int dias) {

    public static DiasDeAtraso entre(LocalDate vencimiento, LocalDate referencia) {
        if (vencimiento == null) {
            throw new IllegalArgumentException("La fecha de vencimiento es obligatoria");
        }
        if (referencia == null) {
            throw new IllegalArgumentException("La fecha de referencia es obligatoria");
        }
        return new DiasDeAtraso((int) ChronoUnit.DAYS.between(vencimiento, referencia));
    }

    public TramoDeGestion tramoDeGestion() {
        if (this.dias < -5) {
            return TramoDeGestion.SIN_ACCION;
        }
        if (this.dias <= 0) {
            return TramoDeGestion.RECORDATORIO;
        }
        if (this.dias <= 15) {
            return TramoDeGestion.LLAMADA_DE_SEGUIMIENTO;
        }
        if (this.dias <= 30) {
            return TramoDeGestion.COMUNICACION_FORMAL;
        }
        return TramoDeGestion.SUSPENSION_DE_CREDITO;
    }

    public boolean superaLosTreinta() {
        return this.dias > 30;
    }
}
