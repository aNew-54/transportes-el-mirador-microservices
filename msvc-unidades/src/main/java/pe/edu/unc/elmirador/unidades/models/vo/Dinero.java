package pe.edu.unc.elmirador.unidades.models.vo;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import pe.edu.unc.elmirador.unidades.exceptions.MonedaIncompatibleException;

@Embeddable
public record Dinero(BigDecimal monto, String codigoMoneda) implements Comparable<Dinero> {

    public Dinero {
        if (monto == null) {
            throw new IllegalArgumentException("El monto no puede ser nulo");
        }
        if (codigoMoneda == null) {
            throw new IllegalArgumentException("El codigo de moneda no puede ser nulo");
        }
        codigoMoneda = codigoMoneda.trim().toUpperCase();
        if (codigoMoneda.length() != 3) {
            throw new IllegalArgumentException("El codigo de moneda debe ser ISO-4217 de 3 letras: " + codigoMoneda);
        }
        if (monto.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El monto no puede ser negativo: " + monto);
        }
        monto = monto.setScale(2, RoundingMode.HALF_UP);
    }

    public Dinero sumar(Dinero otro) {
        if (otro == null) {
            throw new IllegalArgumentException("El dinero a sumar no puede ser nulo");
        }
        if (!this.codigoMoneda.equals(otro.codigoMoneda)) {
            throw new MonedaIncompatibleException(
                    "No se pueden sumar montos de monedas distintas: " + this.codigoMoneda + " y " + otro.codigoMoneda);
        }
        return new Dinero(this.monto.add(otro.monto), this.codigoMoneda);
    }

    public Dinero multiplicarPor(int factor) {
        if (factor < 0) {
            throw new IllegalArgumentException("El factor no puede ser negativo: " + factor);
        }
        return new Dinero(this.monto.multiply(BigDecimal.valueOf(factor)), this.codigoMoneda);
    }

    public static Dinero cero(String codigoMoneda) {
        return new Dinero(BigDecimal.ZERO, codigoMoneda);
    }

    @Override
    public int compareTo(Dinero otro) {
        if (otro == null) {
            throw new IllegalArgumentException("No se puede comparar con un Dinero nulo");
        }
        if (!this.codigoMoneda.equals(otro.codigoMoneda)) {
            throw new MonedaIncompatibleException(
                    "No se pueden comparar montos de monedas distintas: " + this.codigoMoneda + " y " + otro.codigoMoneda);
        }
        return this.monto.compareTo(otro.monto);
    }
}
