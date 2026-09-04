package pe.edu.unc.elmirador.facturacion.models.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.facturacion.exceptions.ImportesInconsistentesException;
import pe.edu.unc.elmirador.facturacion.exceptions.MonedaIncompatibleException;

class DetraccionTest {

    @Test
    @DisplayName("Detraccion: con porcentaje mayor que cero y sin cuenta bancaria lanza IllegalArgumentException")
    void debeRechazarDetraccionMayorACeroSinCuentaBancaria() {
        assertThatThrownBy(() -> new Detraccion(new BigDecimal("4"), Dinero.de("72.86", "PEN"), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("La cuenta bancaria es obligatoria");

        assertThatThrownBy(() -> new Detraccion(new BigDecimal("4"), Dinero.de("72.86", "PEN"), "   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("La cuenta bancaria es obligatoria");
    }

    @Test
    @DisplayName("Detraccion: con porcentaje cero y monto cero la cuenta bancaria puede faltar y no lanza")
    void debePermitirDetraccionCeroSinCuentaBancaria() {
        Detraccion detraccion = Detraccion.sinDetraccion("PEN");

        assertThat(detraccion.porcentaje()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(detraccion.monto()).isEqualTo(Dinero.cero("PEN"));
        assertThat(detraccion.cuentaBancaria()).isNull();
    }

    @Test
    @DisplayName("Detraccion: con porcentaje cero pero monto mayor a cero lanza IllegalArgumentException")
    void debeRechazarDetraccionCeroConMontoPositivo() {
        assertThatThrownBy(() -> new Detraccion(BigDecimal.ZERO, Dinero.de("10.00", "PEN"), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Si el porcentaje de detraccion es cero, el monto debe ser cero");
    }

    @Test
    @DisplayName("Detraccion: porcentaje fuera de [0, 100) lanza IllegalArgumentException")
    void debeRechazarPorcentajeFueraDeRango() {
        assertThatThrownBy(() -> new Detraccion(new BigDecimal("-0.01"), Dinero.cero("PEN"), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("rango [0, 100)");

        assertThatThrownBy(() -> new Detraccion(new BigDecimal("100"), Dinero.de("100.00", "PEN"), "00-123-456"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("rango [0, 100)");
    }

    @Test
    @DisplayName("Detraccion: montoNeto(total) devuelve total menos monto de detraccion")
    void debeCalcularMontoNetoCorrectamente() {
        Detraccion detraccion = new Detraccion(new BigDecimal("4"), Dinero.de("72.86", "PEN"), "00-123-456");
        Dinero total = Dinero.de("1821.60", "PEN");

        Dinero neto = detraccion.montoNeto(total);
        assertThat(neto).isEqualTo(Dinero.de("1748.74", "PEN"));
    }

    @Test
    @DisplayName("Detraccion: montoNeto lanza MonedaIncompatibleException si las monedas difieren")
    void debeRechazarMontoNetoConMonedaDistinta() {
        Detraccion detraccion = new Detraccion(new BigDecimal("4"), Dinero.de("72.86", "PEN"), "00-123-456");
        Dinero totalUsd = Dinero.de("1821.60", "USD");

        assertThatThrownBy(() -> detraccion.montoNeto(totalUsd))
            .isInstanceOf(MonedaIncompatibleException.class);
    }

    @Test
    @DisplayName("Detraccion: montoNeto lanza ImportesInconsistentesException si el monto de detraccion excede el total")
    void debeRechazarMontoNetoSiDetraccionSuperaTotal() {
        Detraccion detraccion = new Detraccion(new BigDecimal("10"), Dinero.de("500.00", "PEN"), "00-123-456");
        Dinero totalMenor = Dinero.de("400.00", "PEN");

        assertThatThrownBy(() -> detraccion.montoNeto(totalMenor))
            .isInstanceOf(ImportesInconsistentesException.class)
            .hasMessageContaining("no puede ser mayor que el total");
    }
}
