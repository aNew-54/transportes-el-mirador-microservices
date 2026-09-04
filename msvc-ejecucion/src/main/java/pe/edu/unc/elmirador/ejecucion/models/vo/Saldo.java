package pe.edu.unc.elmirador.ejecucion.models.vo;

import jakarta.persistence.Embeddable;

/**
 * Objeto de valor inmutable que representa el saldo de una liquidacion.
 * Nunca se almacena en base de datos (LIQ-02).
 */
@Embeddable
public record Saldo(Dinero importe, SignoDeSaldo signo) {

    public Saldo {
        if (importe == null) {
            throw new IllegalArgumentException("El importe del saldo es obligatorio");
        }
        if (signo == null) {
            throw new IllegalArgumentException("El signo del saldo es obligatorio");
        }
    }

    public static Saldo entre(Dinero anticipo, Dinero gastos) {
        if (anticipo == null) {
            throw new IllegalArgumentException("El anticipo es obligatorio para calcular el saldo");
        }
        if (gastos == null) {
            throw new IllegalArgumentException("Los gastos son obligatorios para calcular el saldo");
        }
        if (gastos.esMayorQue(anticipo)) {
            return new Saldo(gastos.restar(anticipo), SignoDeSaldo.A_FAVOR_DEL_CONDUCTOR);
        } else if (anticipo.esMayorQue(gastos)) {
            return new Saldo(anticipo.restar(gastos), SignoDeSaldo.A_FAVOR_DE_LA_EMPRESA);
        } else {
            return new Saldo(Dinero.cero(anticipo.codigoMoneda()), SignoDeSaldo.SALDADO);
        }
    }
}
