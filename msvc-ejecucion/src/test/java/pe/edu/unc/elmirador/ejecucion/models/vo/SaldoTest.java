package pe.edu.unc.elmirador.ejecucion.models.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.ejecucion.exceptions.MonedaIncompatibleException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SaldoTest {

    @Test
    @DisplayName("[LIQ-02] Saldo.entre con gastos mayores al anticipo resulta A_FAVOR_DEL_CONDUCTOR")
    void saldoAFavorDelConductorCuandoGastosSuperanAnticipo_LIQ02() {
        Dinero anticipo = Dinero.de("100.00", "PEN");
        Dinero gastos = Dinero.de("150.00", "PEN");

        Saldo saldo = Saldo.entre(anticipo, gastos);

        assertThat(saldo.signo()).isEqualTo(SignoDeSaldo.A_FAVOR_DEL_CONDUCTOR);
        assertThat(saldo.importe()).isEqualTo(Dinero.de("50.00", "PEN"));
    }

    @Test
    @DisplayName("[LIQ-02] Saldo.entre con anticipo mayor a gastos resulta A_FAVOR_DE_LA_EMPRESA")
    void saldoAFavorDeLaEmpresaCuandoSobraAnticipo_LIQ02() {
        Dinero anticipo = Dinero.de("300.00", "PEN");
        Dinero gastos = Dinero.de("180.50", "PEN");

        Saldo saldo = Saldo.entre(anticipo, gastos);

        assertThat(saldo.signo()).isEqualTo(SignoDeSaldo.A_FAVOR_DE_LA_EMPRESA);
        assertThat(saldo.importe()).isEqualTo(Dinero.de("119.50", "PEN"));
    }

    @Test
    @DisplayName("[LIQ-02] Saldo.entre con anticipo igual a gastos resulta exactamente SALDADO")
    void saldoSaldadoCuandoAnticipoYGastoCoincidenExactamente_LIQ02() {
        Dinero anticipo = Dinero.de("200.00", "PEN");
        Dinero gastos = Dinero.de("200.00", "PEN");

        Saldo saldo = Saldo.entre(anticipo, gastos);

        assertThat(saldo.signo()).isEqualTo(SignoDeSaldo.SALDADO);
        assertThat(saldo.importe().esCero()).isTrue();
        assertThat(saldo.importe().codigoMoneda()).isEqualTo("PEN");
    }

    @Test
    @DisplayName("[LIQ-02] Saldo.entre con monedas incompatibles lanza MonedaIncompatibleException")
    void saldoEntreMonedasDistintasLanzaExcepcion() {
        Dinero anticipo = Dinero.de("100.00", "PEN");
        Dinero gastos = Dinero.de("100.00", "USD");

        assertThatThrownBy(() -> Saldo.entre(anticipo, gastos))
                .isInstanceOf(MonedaIncompatibleException.class)
                .hasMessageContaining("monedas distintas");
    }

    @Test
    @DisplayName("Saldo.entre con valores nulos lanza IllegalArgumentException")
    void saldoEntreValoresNulosLanzaExcepcion() {
        Dinero dinero = Dinero.de("50.00", "PEN");

        assertThatThrownBy(() -> Saldo.entre(null, dinero))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Saldo.entre(dinero, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Constructor de Saldo con argumentos nulos lanza IllegalArgumentException")
    void constructorSaldoConNulosLanzaExcepcion() {
        Dinero dinero = Dinero.de("50.00", "PEN");

        assertThatThrownBy(() -> new Saldo(null, SignoDeSaldo.SALDADO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Saldo(dinero, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
