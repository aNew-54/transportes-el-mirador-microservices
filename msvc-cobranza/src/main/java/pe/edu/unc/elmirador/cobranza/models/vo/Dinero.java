package pe.edu.unc.elmirador.cobranza.models.vo;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import pe.edu.unc.elmirador.cobranza.exceptions.MonedaIncompatibleException;

/**
 * Objeto de valor inmutable que representa un importe monetario con su codigo ISO-4217.
 * Escala normalizada a 2 decimales y monto no negativo.
 */
@Embeddable
public record Dinero(BigDecimal monto, String codigoMoneda) {

    public Dinero {
        if (monto == null) {
            throw new IllegalArgumentException("El monto es obligatorio");
        }
        if (codigoMoneda == null || codigoMoneda.isBlank()) {
            throw new IllegalArgumentException("El codigo de moneda es obligatorio");
        }
        String monedaNormalizada = codigoMoneda.trim().toUpperCase();
        if (!monedaNormalizada.matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException("El codigo de moneda debe ser ISO-4217 de 3 letras: " + codigoMoneda);
        }
        if (monto.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El monto no puede ser negativo: " + monto);
        }
        monto = monto.setScale(2, RoundingMode.HALF_UP);
        codigoMoneda = monedaNormalizada;
    }

    public static Dinero de(String monto, String codigoMoneda) {
        if (monto == null || monto.isBlank()) {
            throw new IllegalArgumentException("El monto es obligatorio");
        }
        return new Dinero(new BigDecimal(monto), codigoMoneda);
    }

    public static Dinero de(BigDecimal monto, String codigoMoneda) {
        return new Dinero(monto, codigoMoneda);
    }

    public static Dinero cero(String codigoMoneda) {
        return new Dinero(BigDecimal.ZERO, codigoMoneda);
    }

    public Dinero sumar(Dinero otro) {
        validarMismaMoneda(otro);
        return new Dinero(this.monto.add(otro.monto), this.codigoMoneda);
    }

    public Dinero restar(Dinero otro) {
        validarMismaMoneda(otro);
        if (this.monto.compareTo(otro.monto) < 0) {
            throw new IllegalArgumentException(
                "No se puede restar un monto mayor: " + otro.monto + " > " + this.monto
            );
        }
        return new Dinero(this.monto.subtract(otro.monto), this.codigoMoneda);
    }

    public boolean esCero() {
        return this.monto.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean esMayorQue(Dinero otro) {
        validarMismaMoneda(otro);
        return this.monto.compareTo(otro.monto) > 0;
    }

    public boolean esMayorOIgualQue(Dinero otro) {
        validarMismaMoneda(otro);
        return this.monto.compareTo(otro.monto) >= 0;
    }

    public boolean esMenorQue(Dinero otro) {
        validarMismaMoneda(otro);
        return this.monto.compareTo(otro.monto) < 0;
    }

    private void validarMismaMoneda(Dinero otro) {
        if (otro == null) {
            throw new IllegalArgumentException("El dinero a operar es obligatorio");
        }
        if (!this.codigoMoneda.equalsIgnoreCase(otro.codigoMoneda)) {
            throw new MonedaIncompatibleException(
                "No se pueden operar monedas distintas: " + this.codigoMoneda + " y " + otro.codigoMoneda
            );
        }
    }
}
