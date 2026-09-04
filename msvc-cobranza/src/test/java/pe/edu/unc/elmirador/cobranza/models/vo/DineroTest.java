package pe.edu.unc.elmirador.cobranza.models.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.cobranza.exceptions.MonedaIncompatibleException;

class DineroTest {

    @Test
    @DisplayName("Normaliza la escala a dos decimales y convierte codigo de moneda a mayusculas")
    void debeNormalizarEscalaYMoneda() {
        Dinero dinero = new Dinero(new BigDecimal("100.5"), "pen");
        assertThat(dinero.monto()).isEqualByComparingTo("100.50");
        assertThat(dinero.monto().scale()).isEqualTo(2);
        assertThat(dinero.codigoMoneda()).isEqualTo("PEN");
    }

    @Test
    @DisplayName("Lanza IllegalArgumentException si el monto es negativo")
    void debeRechazarMontoNegativo() {
        assertThatThrownBy(() -> new Dinero(new BigDecimal("-0.01"), "PEN"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no puede ser negativo");
    }

    @Test
    @DisplayName("Lanza IllegalArgumentException si el monto es nulo")
    void debeRechazarMontoNulo() {
        assertThatThrownBy(() -> new Dinero(null, "PEN"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("monto es obligatorio");
    }

    @Test
    @DisplayName("Lanza IllegalArgumentException si el codigo de moneda es nulo o en blanco")
    void debeRechazarMonedaNulaOEnBlanco() {
        assertThatThrownBy(() -> new Dinero(BigDecimal.TEN, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("codigo de moneda es obligatorio");

        assertThatThrownBy(() -> new Dinero(BigDecimal.TEN, "  "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("codigo de moneda es obligatorio");
    }

    @Test
    @DisplayName("Lanza IllegalArgumentException si la moneda no es ISO-4217 de 3 letras")
    void debeRechazarMonedaInvalida() {
        assertThatThrownBy(() -> new Dinero(BigDecimal.TEN, "US"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ISO-4217");

        assertThatThrownBy(() -> new Dinero(BigDecimal.TEN, "SOLES"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ISO-4217");
    }

    @Test
    @DisplayName("Suma correctamente importes de la misma moneda")
    void debeSumarMismaMoneda() {
        Dinero d1 = Dinero.de("100.50", "PEN");
        Dinero d2 = Dinero.de("49.50", "PEN");

        Dinero resultado = d1.sumar(d2);

        assertThat(resultado.monto()).isEqualByComparingTo("150.00");
        assertThat(resultado.codigoMoneda()).isEqualTo("PEN");
    }

    @Test
    @DisplayName("Lanza MonedaIncompatibleException al sumar monedas distintas")
    void debeFallarAlSumarMonedasDistintas() {
        Dinero soles = Dinero.de("100.00", "PEN");
        Dinero dolares = Dinero.de("100.00", "USD");

        assertThatThrownBy(() -> soles.sumar(dolares))
            .isInstanceOf(MonedaIncompatibleException.class)
            .hasMessageContaining("No se pueden operar monedas distintas");
    }

    @Test
    @DisplayName("Resta correctamente importes de la misma moneda")
    void debeRestarMismaMoneda() {
        Dinero d1 = Dinero.de("150.00", "PEN");
        Dinero d2 = Dinero.de("50.00", "PEN");

        Dinero resultado = d1.restar(d2);

        assertThat(resultado.monto()).isEqualByComparingTo("100.00");
        assertThat(resultado.codigoMoneda()).isEqualTo("PEN");
    }

    @Test
    @DisplayName("Lanza MonedaIncompatibleException al restar monedas distintas")
    void debeFallarAlRestarMonedasDistintas() {
        Dinero soles = Dinero.de("100.00", "PEN");
        Dinero dolares = Dinero.de("50.00", "USD");

        assertThatThrownBy(() -> soles.restar(dolares))
            .isInstanceOf(MonedaIncompatibleException.class)
            .hasMessageContaining("No se pueden operar monedas distintas");
    }

    @Test
    @DisplayName("Lanza IllegalArgumentException al restar un monto mayor que dejaria saldo negativo")
    void debeFallarAlRestarMontoMayor() {
        Dinero d1 = Dinero.de("50.00", "PEN");
        Dinero d2 = Dinero.de("100.00", "PEN");

        assertThatThrownBy(() -> d1.restar(d2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No se puede restar un monto mayor");
    }

    @Test
    @DisplayName("Evalua esCero correctamente")
    void debeEvaluarEsCero() {
        assertThat(Dinero.cero("PEN").esCero()).isTrue();
        assertThat(Dinero.de("0.00", "PEN").esCero()).isTrue();
        assertThat(Dinero.de("0.01", "PEN").esCero()).isFalse();
    }

    @Test
    @DisplayName("Evalua esMayorQue y esMayorOIgualQue correctamente")
    void debeEvaluarMayorQue() {
        Dinero d1 = Dinero.de("100.00", "PEN");
        Dinero d2 = Dinero.de("50.00", "PEN");
        Dinero d3 = Dinero.de("100.00", "PEN");

        assertThat(d1.esMayorQue(d2)).isTrue();
        assertThat(d2.esMayorQue(d1)).isFalse();
        assertThat(d1.esMayorQue(d3)).isFalse();
        assertThat(d1.esMayorOIgualQue(d3)).isTrue();
    }

    @Test
    @DisplayName("Lanza MonedaIncompatibleException al comparar mayorQue entre monedas distintas")
    void debeFallarAlCompararMonedasDistintas() {
        Dinero soles = Dinero.de("100.00", "PEN");
        Dinero dolares = Dinero.de("50.00", "USD");

        assertThatThrownBy(() -> soles.esMayorQue(dolares))
            .isInstanceOf(MonedaIncompatibleException.class);
    }
}
