package pe.edu.unc.elmirador.unidades.models.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.unidades.exceptions.MonedaIncompatibleException;

class DineroTest {

    @Test
    @DisplayName("Borde: Dinero sumando monedas distintas lanza MonedaIncompatibleException")
    void sumarMonedasDistintasLanzaMonedaIncompatibleException() {
        Dinero soles = new Dinero(new BigDecimal("100.00"), "PEN");
        Dinero dolares = new Dinero(new BigDecimal("50.00"), "USD");

        assertThatThrownBy(() -> soles.sumar(dolares))
                .isInstanceOf(MonedaIncompatibleException.class)
                .hasMessageContaining("PEN")
                .hasMessageContaining("USD");
    }

    @Test
    @DisplayName("Sumar dineros de la misma moneda produce la suma correcta con escala 2")
    void sumarMismaMonedaCalculaTotalCorrecto() {
        Dinero monto1 = new Dinero(new BigDecimal("150.50"), "PEN");
        Dinero monto2 = new Dinero(new BigDecimal("49.50"), "PEN");

        Dinero total = monto1.sumar(monto2);

        assertThat(total.monto().compareTo(new BigDecimal("200.00"))).isZero();
        assertThat(total.codigoMoneda()).isEqualTo("PEN");
        assertThat(total.monto().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Multiplicar por factor entero calcula el producto correcto")
    void multiplicarPorFactorPositivo() {
        Dinero unitario = new Dinero(new BigDecimal("25.25"), "PEN");

        Dinero total = unitario.multiplicarPor(4);

        assertThat(total.monto().compareTo(new BigDecimal("101.00"))).isZero();
        assertThat(total.codigoMoneda()).isEqualTo("PEN");
    }

    @Test
    @DisplayName("Monto conserva escala de 2 decimales")
    void montoConservaEscalaDosDecimales() {
        Dinero dinero = new Dinero(new BigDecimal("10"), "USD");

        assertThat(dinero.monto().scale()).isEqualTo(2);
        assertThat(dinero.monto().compareTo(new BigDecimal("10.00"))).isZero();
    }

    @Test
    @DisplayName("Monto negativo lanza IllegalArgumentException")
    void montoNegativoLanzaExcepcion() {
        assertThatThrownBy(() -> new Dinero(new BigDecimal("-0.01"), "PEN"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Codigo de moneda invalido lanza IllegalArgumentException")
    void codigoMonedaInvalidoLanzaExcepcion() {
        assertThatThrownBy(() -> new Dinero(BigDecimal.TEN, "PE"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Dinero(BigDecimal.TEN, "SOLES"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Dinero(BigDecimal.TEN, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("compareTo compara montos de la misma moneda")
    void compareToComparaMontosDeMismaMoneda() {
        Dinero d1 = new Dinero(new BigDecimal("100.00"), "PEN");
        Dinero d2 = new Dinero(new BigDecimal("200.00"), "PEN");

        assertThat(d1.compareTo(d2)).isNegative();
        assertThat(d2.compareTo(d1)).isPositive();
        assertThat(d1.compareTo(new Dinero(new BigDecimal("100.000"), "PEN"))).isZero();
    }

    @Test
    @DisplayName("compareTo con monedas distintas lanza MonedaIncompatibleException")
    void compareToMonedasDistintasLanzaMonedaIncompatibleException() {
        Dinero d1 = new Dinero(new BigDecimal("100.00"), "PEN");
        Dinero d2 = new Dinero(new BigDecimal("100.00"), "USD");

        assertThatThrownBy(() -> d1.compareTo(d2))
                .isInstanceOf(MonedaIncompatibleException.class);
    }
}
