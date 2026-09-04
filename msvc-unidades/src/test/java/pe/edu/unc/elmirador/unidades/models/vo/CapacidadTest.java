package pe.edu.unc.elmirador.unidades.models.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CapacidadTest {

    @Test
    @DisplayName("Borde: admite con el peso exacto y con el volumen exacto del maximo retorna true")
    void admiteConPesoYVolumenExactosAlMaximoRetornaTrue() {
        Capacidad capacidad = new Capacidad(10_000, new BigDecimal("32.00"));

        boolean resultado = capacidad.admite(10_000, new BigDecimal("32.00"));

        assertThat(resultado).isTrue();
    }

    @Test
    @DisplayName("admite con peso y volumen estrictamente menores retorna true")
    void admiteConValoresDentroDelLimiteRetornaTrue() {
        Capacidad capacidad = new Capacidad(10_000, new BigDecimal("32.00"));

        boolean resultado = capacidad.admite(5_000, new BigDecimal("15.50"));

        assertThat(resultado).isTrue();
    }

    @Test
    @DisplayName("admite con peso superior al maximo retorna false")
    void admiteConPesoSuperiorAlMaximoRetornaFalse() {
        Capacidad capacidad = new Capacidad(10_000, new BigDecimal("32.00"));

        boolean resultado = capacidad.admite(10_001, new BigDecimal("32.00"));

        assertThat(resultado).isFalse();
    }

    @Test
    @DisplayName("admite con volumen superior al maximo retorna false")
    void admiteConVolumenSuperiorAlMaximoRetornaFalse() {
        Capacidad capacidad = new Capacidad(10_000, new BigDecimal("32.00"));

        boolean resultado = capacidad.admite(10_000, new BigDecimal("32.01"));

        assertThat(resultado).isFalse();
    }

    @Test
    @DisplayName("admite con peso negativo retorna false")
    void admiteConPesoNegativoRetornaFalse() {
        Capacidad capacidad = new Capacidad(10_000, new BigDecimal("32.00"));

        boolean resultado = capacidad.admite(-1, new BigDecimal("10.00"));

        assertThat(resultado).isFalse();
    }

    @Test
    @DisplayName("admite con volumen nulo retorna false")
    void admiteConVolumenNuloRetornaFalse() {
        Capacidad capacidad = new Capacidad(10_000, new BigDecimal("32.00"));

        boolean resultado = capacidad.admite(5_000, null);

        assertThat(resultado).isFalse();
    }

    @Test
    @DisplayName("Crear capacidad con peso cero o negativo lanza IllegalArgumentException")
    void crearConPesoCeroONegativoLanzaExcepcion() {
        assertThatThrownBy(() -> new Capacidad(0, new BigDecimal("10.00")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Capacidad(-100, new BigDecimal("10.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Crear capacidad con volumen nulo, cero o negativo lanza IllegalArgumentException")
    void crearConVolumenCeroONegativoLanzaExcepcion() {
        assertThatThrownBy(() -> new Capacidad(1000, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Capacidad(1000, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Capacidad(1000, new BigDecimal("-5.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
