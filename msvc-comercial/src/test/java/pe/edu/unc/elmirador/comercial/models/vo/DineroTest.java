package pe.edu.unc.elmirador.comercial.models.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.comercial.exceptions.MonedaIncompatibleException;

class DineroTest {

    @Test
    @DisplayName("Dinero: Suma, resta, multiplicacion y mitad operan con precision de 2 decimales")
    void debeRealizarOperacionesAritmeticasBasicas() {
        Dinero d1 = Dinero.de("100.50", "PEN");
        Dinero d2 = Dinero.de("49.50", "PEN");

        assertThat(d1.sumar(d2)).isEqualTo(Dinero.de("150.00", "PEN"));
        assertThat(d1.restar(d2)).isEqualTo(Dinero.de("51.00", "PEN"));
        assertThat(d1.multiplicarPor(new BigDecimal("2"))).isEqualTo(Dinero.de("201.00", "PEN"));
        assertThat(d1.mitad()).isEqualTo(Dinero.de("50.25", "PEN"));
    }

    @Test
    @DisplayName("Dinero: Operar con monedas incompatibles lanza MonedaIncompatibleException")
    void debeRechazarOperacionesEntreMonedasDistintas() {
        Dinero soles = Dinero.de("100.00", "PEN");
        Dinero dolares = Dinero.de("100.00", "USD");

        assertThatThrownBy(() -> soles.sumar(dolares))
            .isInstanceOf(MonedaIncompatibleException.class)
            .hasMessageContaining("PEN")
            .hasMessageContaining("USD");

        assertThatThrownBy(() -> soles.restar(dolares))
            .isInstanceOf(MonedaIncompatibleException.class);

        assertThatThrownBy(() -> soles.esMayorQue(dolares))
            .isInstanceOf(MonedaIncompatibleException.class);
    }

    @Test
    @DisplayName("Dinero: Monto negativo lanza IllegalArgumentException")
    void debeRechazarMontoNegativo() {
        assertThatThrownBy(() -> Dinero.de("-10.00", "PEN"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("no puede ser negativo");
    }

    @Test
    @DisplayName("Dinero: Codigo de moneda invalido o nulo lanza IllegalArgumentException (regla D4)")
    void debeRechazarMonedaInvalidaONula() {
        assertThatThrownBy(() -> Dinero.de("100.00", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("codigo de moneda es obligatorio");

        assertThatThrownBy(() -> Dinero.de("100.00", "PE"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ISO-4217");
    }

    @Test
    @DisplayName("Dinero: Restar un monto mayor lanza IllegalArgumentException")
    void debeRechazarRestaConResultadoNegativo() {
        Dinero d1 = Dinero.de("50.00", "PEN");
        Dinero d2 = Dinero.de("60.00", "PEN");

        assertThatThrownBy(() -> d1.restar(d2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No se puede restar un monto mayor");
    }

    @Test
    @DisplayName("Dinero: esMayorQue, esMayorOIgualQue, esMenorQue y esCero comparan correctamente con BigDecimal.compareTo")
    void debeCompararCorrectamenteMontos() {
        Dinero d100 = Dinero.de("100.00", "PEN");
        Dinero d50 = Dinero.de("50.00", "PEN");
        Dinero cero = Dinero.cero("PEN");

        assertThat(d100.esMayorQue(d50)).isTrue();
        assertThat(d50.esMenorQue(d100)).isTrue();
        assertThat(d100.esMayorOIgualQue(d100)).isTrue();
        assertThat(cero.esCero()).isTrue();
        assertThat(d50.esCero()).isFalse();
    }
}
