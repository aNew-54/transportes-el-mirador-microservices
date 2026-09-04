package pe.edu.unc.elmirador.comercial.models.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pe.edu.unc.elmirador.comercial.exceptions.RucInvalidoException;

class RucTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "10123456789",
        "15123456789",
        "17123456789",
        "20123456789"
    })
    @DisplayName("RUC es valido para los cuatro prefijos permitidos (10, 15, 17, 20) con 11 digitos")
    void debeAceptarPrefijosValidos(String rucValido) {
        Ruc ruc = new Ruc(rucValido);
        assertThat(ruc.valor()).isEqualTo(rucValido);
    }

    @Test
    @DisplayName("RUC con prefijo invalido no permitido lanza RucInvalidoException")
    void debeRechazarPrefijoInvalido() {
        assertThatThrownBy(() -> new Ruc("25123456789"))
            .isInstanceOf(RucInvalidoException.class)
            .hasMessageContaining("comenzar con 10, 15, 17 o 20");
    }

    @Test
    @DisplayName("RUC con 10 digitos (longitud insuficiente) lanza RucInvalidoException")
    void debeRechazarDiezDigitos() {
        assertThatThrownBy(() -> new Ruc("2012345678"))
            .isInstanceOf(RucInvalidoException.class)
            .hasMessageContaining("11 digitos numericos");
    }

    @Test
    @DisplayName("RUC con 12 digitos (longitud excedida) lanza RucInvalidoException")
    void debeRechazarDoceDigitos() {
        assertThatThrownBy(() -> new Ruc("201234567890"))
            .isInstanceOf(RucInvalidoException.class)
            .hasMessageContaining("11 digitos numericos");
    }

    @Test
    @DisplayName("RUC nulo o en blanco lanza RucInvalidoException")
    void debeRechazarRucNuloOVacio() {
        assertThatThrownBy(() -> new Ruc(null))
            .isInstanceOf(RucInvalidoException.class)
            .hasMessageContaining("obligatorio");

        assertThatThrownBy(() -> new Ruc("   "))
            .isInstanceOf(RucInvalidoException.class)
            .hasMessageContaining("obligatorio");
    }

    @Test
    @DisplayName("RUC con caracteres no numericos lanza RucInvalidoException")
    void debeRechazarCaracteresNoNumericos() {
        assertThatThrownBy(() -> new Ruc("2012345678A"))
            .isInstanceOf(RucInvalidoException.class);
    }
}
