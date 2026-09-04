package pe.edu.unc.elmirador.facturacion.models.vo;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import pe.edu.unc.elmirador.facturacion.exceptions.ImportesInconsistentesException;
import pe.edu.unc.elmirador.facturacion.exceptions.MonedaIncompatibleException;

/**
 * Objeto de valor inmutable que representa el regimen de detracciones SUNAT.
 * Porcentaje en [0, 100). Si porcentaje es cero, el monto debe ser cero y la cuenta puede faltar.
 * Si porcentaje es mayor que cero, la cuenta bancaria en el Banco de la Nacion es obligatoria.
 */
@Embeddable
public record Detraccion(BigDecimal porcentaje, Dinero monto, String cuentaBancaria) {

    public Detraccion {
        if (porcentaje == null) {
            throw new IllegalArgumentException("El porcentaje de detraccion es obligatorio");
        }
        if (porcentaje.compareTo(BigDecimal.ZERO) < 0 || porcentaje.compareTo(BigDecimal.valueOf(100)) >= 0) {
            throw new IllegalArgumentException(
                "El porcentaje de detraccion debe pertenecer al rango [0, 100): " + porcentaje
            );
        }
        if (monto == null) {
            throw new IllegalArgumentException("El monto de detraccion es obligatorio");
        }

        if (porcentaje.compareTo(BigDecimal.ZERO) == 0) {
            if (!monto.esCero()) {
                throw new IllegalArgumentException("Si el porcentaje de detraccion es cero, el monto debe ser cero");
            }
            cuentaBancaria = cuentaBancaria != null && !cuentaBancaria.isBlank() ? cuentaBancaria.trim() : null;
        } else {
            if (cuentaBancaria == null || cuentaBancaria.isBlank()) {
                throw new IllegalArgumentException(
                    "La cuenta bancaria es obligatoria cuando el porcentaje de detraccion es mayor que cero"
                );
            }
            cuentaBancaria = cuentaBancaria.trim();
        }
    }

    public static Detraccion sinDetraccion(String codigoMoneda) {
        if (codigoMoneda == null || codigoMoneda.isBlank()) {
            throw new IllegalArgumentException("El codigo de moneda es obligatorio");
        }
        return new Detraccion(BigDecimal.ZERO, Dinero.cero(codigoMoneda), null);
    }

    public static Detraccion de(BigDecimal porcentaje, Dinero monto, String cuentaBancaria) {
        return new Detraccion(porcentaje, monto, cuentaBancaria);
    }

    public Dinero montoNeto(Dinero total) {
        if (total == null) {
            throw new IllegalArgumentException("El total es obligatorio");
        }
        if (!this.monto.codigoMoneda().equalsIgnoreCase(total.codigoMoneda())) {
            throw new MonedaIncompatibleException(
                "La moneda de la detraccion (" + this.monto.codigoMoneda() + ") no coincide con la del total (" + total.codigoMoneda() + ")"
            );
        }
        if (this.monto.esMayorQue(total)) {
            throw new ImportesInconsistentesException(
                "El monto de detraccion (" + this.monto.monto() + ") no puede ser mayor que el total (" + total.monto() + ")"
            );
        }
        return total.restar(this.monto);
    }
}
