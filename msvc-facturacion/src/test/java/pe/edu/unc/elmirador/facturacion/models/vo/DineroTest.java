package pe.edu.unc.elmirador.facturacion.models.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.facturacion.exceptions.MonedaIncompatibleException;

class DineroTest {

    @Test
    @DisplayName("Dinero: debe crear instancia con monto no negativo y escala normalizada a 2 decimales")
    void debeCrearDineroValidoConEscalaDosDecimales() {
        Dinero dinero = Dinero.de("150.5", "PEN");

        assertThat(dinero.monto()).isEqualByComparingTo("150.50");
        assertThat(dinero.monto().scale()).isEqualTo(2);
        assertThat(dinero.codigoMoneda()).isEqualTo("PEN");
        assertThat(dinero.esCero()).isFalse();
    }

    @Test
    @DisplayName("Dinero: debe rechazar monto nulo o negativo")
    void debeRechazarMontoNuloONegativo() {
        assertThatThrownBy(() -> new Dinero(null, "PEN"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("El monto es obligatorio");

        assertThatThrownBy(() -> Dinero.de("-0.01", "PEN"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("El monto no puede ser negativo");
    }

    @Test
    @DisplayName("Dinero: debe rechazar codigo de moneda nulo, vacio o que no cumpla ISO-4217")
    void debeRechazarCodigoMonedaInvalidoONulo() {
        assertThatThrownBy(() -> Dinero.de("10.00", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("El codigo de moneda es obligatorio");

        assertThatThrownBy(() -> Dinero.de("10.00", "   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("El codigo de moneda es obligatorio");

        assertThatThrownBy(() -> Dinero.de("10.00", "SOLES"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("El codigo de moneda debe ser ISO-4217");
    }

    @Test
    @DisplayName("Dinero: debe sumar y restar importes con la misma moneda")
    void debeSumarYRestarImportesMismaMoneda() {
        Dinero d1 = Dinero.de("100.25", "PEN");
        Dinero d2 = Dinero.de("50.75", "PEN");

        Dinero suma = d1.sumar(d2);
        assertThat(suma).isEqualTo(Dinero.de("151.00", "PEN"));

        Dinero resta = d1.restar(d2);
        assertThat(resta).isEqualTo(Dinero.de("49.50", "PEN"));
    }

    @Test
    @DisplayName("Dinero: debe rechazar restar un monto mayor")
    void debeRechazarRestaSiSustraendoEsMayor() {
        Dinero menor = Dinero.de("10.00", "PEN");
        Dinero mayor = Dinero.de("20.00", "PEN");

        assertThatThrownBy(() -> menor.restar(mayor))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No se puede restar un monto mayor");
    }

    @Test
    @DisplayName("Dinero: debe lanzar MonedaIncompatibleException al operar monedas distintas")
    void debeRechazarOperacionesEntreMonedasDistintas() {
        Dinero pen = Dinero.de("100.00", "PEN");
        Dinero usd = Dinero.de("50.00", "USD");

        assertThatThrownBy(() -> pen.sumar(usd))
            .isInstanceOf(MonedaIncompatibleException.class)
            .hasMessageContaining("No se pueden operar monedas distintas");

        assertThatThrownBy(() -> pen.restar(usd))
            .isInstanceOf(MonedaIncompatibleException.class);

        assertThatThrownBy(() -> pen.esMayorQue(usd))
            .isInstanceOf(MonedaIncompatibleException.class);
    }

    @Test
    @DisplayName("Dinero: porcentaje debe calcular correctamente con redondeo HALF_UP")
    void debeCalcularPorcentajeConRedondeoMedioArriba() {
        Dinero total = Dinero.de("1821.60", "PEN");
        Dinero cuatroPorCiento = total.porcentaje(new BigDecimal("4"));

        // 1821.60 * 4 / 100 = 72.864 -> 72.86
        assertThat(cuatroPorCiento).isEqualTo(Dinero.de("72.86", "PEN"));

        Dinero ceroPorCiento = total.porcentaje(BigDecimal.ZERO);
        assertThat(ceroPorCiento).isEqualTo(Dinero.cero("PEN"));
    }

    @Test
    @DisplayName("Dinero: debe comparar importes correctamente")
    void debeCompararImportesCorrectamente() {
        Dinero cien = Dinero.de("100.00", "PEN");
        Dinero cincuenta = Dinero.de("50.00", "PEN");
        Dinero otroCien = Dinero.de("100.00", "PEN");

        assertThat(cien.esMayorQue(cincuenta)).isTrue();
        assertThat(cincuenta.esMayorQue(cien)).isFalse();
        assertThat(cien.esMayorOIgualQue(otroCien)).isTrue();
        assertThat(cincuenta.esMenorQue(cien)).isTrue();
    }
}
