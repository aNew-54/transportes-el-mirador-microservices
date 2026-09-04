package pe.edu.unc.elmirador.comercial.models.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.comercial.exceptions.DescuentoNoAutorizadoException;

class DescuentoTest {

    @Test
    @DisplayName("COT-02: Descuento con 4% viola el rango minimo permitido y lanza DescuentoNoAutorizadoException")
    void cot02_descuentoCuatroPorcientoLanzaExcepcion() {
        assertThatThrownBy(() -> new Descuento(new BigDecimal("4"), "GERENCIA_COMERCIAL"))
            .isInstanceOf(DescuentoNoAutorizadoException.class)
            .hasMessageContaining("entre 5% y 15%");
    }

    @Test
    @DisplayName("COT-02: Descuento con 16% excede el rango maximo permitido y lanza DescuentoNoAutorizadoException")
    void cot02_descuentoDieciseisPorcientoLanzaExcepcion() {
        assertThatThrownBy(() -> new Descuento(new BigDecimal("16"), "GERENCIA_COMERCIAL"))
            .isInstanceOf(DescuentoNoAutorizadoException.class)
            .hasMessageContaining("entre 5% y 15%");
    }

    @Test
    @DisplayName("COT-02: Descuento en borde inferior exacto de 5% es valido y no lanza excepcion")
    void cot02_descuentoCincoPorcientoEsValido() {
        Descuento descuento = new Descuento(new BigDecimal("5"), "GERENCIA_COMERCIAL");
        assertThat(descuento.porcentaje()).isEqualByComparingTo("5");
        assertThat(descuento.autorizadoPor()).isEqualTo("GERENCIA_COMERCIAL");
    }

    @Test
    @DisplayName("COT-02: Descuento en borde superior exacto de 15% es valido y no lanza excepcion")
    void cot02_descuentoQuincePorcientoEsValido() {
        Descuento descuento = new Descuento(new BigDecimal("15"), "GERENCIA_COMERCIAL");
        assertThat(descuento.porcentaje()).isEqualByComparingTo("15");
        assertThat(descuento.autorizadoPor()).isEqualTo("GERENCIA_COMERCIAL");
    }

    @Test
    @DisplayName("COT-02: Descuento sin autorizacion de gerencia (nulo) viola la invariante y lanza DescuentoNoAutorizadoException")
    void cot02_descuentoSinAutorizacionNulaLanzaExcepcion() {
        assertThatThrownBy(() -> new Descuento(new BigDecimal("10"), null))
            .isInstanceOf(DescuentoNoAutorizadoException.class)
            .hasMessageContaining("autorizacion de gerencia");
    }

    @Test
    @DisplayName("COT-02: Descuento con autorizacion de gerencia en blanco viola la invariante y lanza DescuentoNoAutorizadoException")
    void cot02_descuentoConAutorizacionEnBlancoLanzaExcepcion() {
        assertThatThrownBy(() -> new Descuento(new BigDecimal("10"), "    "))
            .isInstanceOf(DescuentoNoAutorizadoException.class)
            .hasMessageContaining("autorizacion de gerencia");
    }

    @Test
    @DisplayName("Descuento con porcentaje nulo lanza IllegalArgumentException")
    void debeRechazarPorcentajeNulo() {
        assertThatThrownBy(() -> new Descuento(null, "GERENCIA_COMERCIAL"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("porcentaje de descuento es obligatorio");
    }
}
