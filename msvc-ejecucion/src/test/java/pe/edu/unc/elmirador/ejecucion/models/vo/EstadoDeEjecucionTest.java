package pe.edu.unc.elmirador.ejecucion.models.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.edu.unc.elmirador.ejecucion.exceptions.TransicionDeEjecucionInvalidaException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EstadoDeEjecucionTest {

    @ParameterizedTest
    @CsvSource({
            "PENDIENTE, EN_RUTA",
            "EN_RUTA, SUSPENDIDA",
            "EN_RUTA, ENTREGADA",
            "SUSPENDIDA, EN_RUTA",
            "ENTREGADA, CERRADA"
    })
    @DisplayName("Transiciones permitidas son validas y no lanzan excepcion")
    void transicionesPermitidasSonValidas(EstadoDeEjecucion origen, EstadoDeEjecucion destino) {
        assertThat(origen.puedeTransicionarHacia(destino)).isTrue();
        origen.validarTransicionHacia(destino);
    }

    @ParameterizedTest
    @CsvSource({
            "PENDIENTE, PENDIENTE",
            "PENDIENTE, SUSPENDIDA",
            "PENDIENTE, ENTREGADA",
            "PENDIENTE, CERRADA",
            "EN_RUTA, PENDIENTE",
            "EN_RUTA, EN_RUTA",
            "EN_RUTA, CERRADA",
            "SUSPENDIDA, PENDIENTE",
            "SUSPENDIDA, SUSPENDIDA",
            "SUSPENDIDA, ENTREGADA",
            "SUSPENDIDA, CERRADA",
            "ENTREGADA, PENDIENTE",
            "ENTREGADA, EN_RUTA",
            "ENTREGADA, SUSPENDIDA",
            "ENTREGADA, ENTREGADA",
            "CERRADA, PENDIENTE",
            "CERRADA, EN_RUTA",
            "CERRADA, SUSPENDIDA",
            "CERRADA, ENTREGADA",
            "CERRADA, CERRADA"
    })
    @DisplayName("Transiciones prohibidas lanzan TransicionDeEjecucionInvalidaException una a una")
    void transicionesProhibidasLanzanExcepcion(EstadoDeEjecucion origen, EstadoDeEjecucion destino) {
        assertThat(origen.puedeTransicionarHacia(destino)).isFalse();
        assertThatThrownBy(() -> origen.validarTransicionHacia(destino))
                .isInstanceOf(TransicionDeEjecucionInvalidaException.class)
                .hasMessageContaining("Transicion invalida");
    }

    @Test
    @DisplayName("Validar transicion con estado destino nulo lanza IllegalArgumentException")
    void destinoNuloLanzaExcepcion() {
        assertThat(EstadoDeEjecucion.PENDIENTE.puedeTransicionarHacia(null)).isFalse();
        assertThatThrownBy(() -> EstadoDeEjecucion.PENDIENTE.validarTransicionHacia(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("destino es obligatorio");
    }
}
