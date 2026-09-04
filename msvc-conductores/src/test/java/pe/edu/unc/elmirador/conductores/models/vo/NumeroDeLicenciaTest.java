package pe.edu.unc.elmirador.conductores.models.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pe.edu.unc.elmirador.conductores.exceptions.NumeroDeLicenciaInvalidoException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NumeroDeLicenciaTest {

    @Test
    @DisplayName("Crea un NumeroDeLicencia valido en formato estandar peruano")
    void crearNumeroDeLicenciaValido() {
        NumeroDeLicencia licencia = new NumeroDeLicencia("Q12345678");
        assertThat(licencia.valor()).isEqualTo("Q12345678");
    }

    @Test
    @DisplayName("Normaliza la letra de minuscula a mayuscula")
    void normalizaMinusculasAMayusculas() {
        NumeroDeLicencia licencia = new NumeroDeLicencia("q12345678");
        assertThat(licencia.valor()).isEqualTo("Q12345678");
    }

    @Test
    @DisplayName("Elimina espacios en blanco al inicio y al final")
    void ignoraEspaciosAlrededor() {
        NumeroDeLicencia licencia = new NumeroDeLicencia("   Q87654321   ");
        assertThat(licencia.valor()).isEqualTo("Q87654321");
    }

    @Test
    @DisplayName("Valor nulo lanza NumeroDeLicenciaInvalidoException")
    void valorNuloLanzaExcepcion() {
        assertThatThrownBy(() -> new NumeroDeLicencia(null))
                .isInstanceOf(NumeroDeLicenciaInvalidoException.class)
                .hasMessageContaining("no puede ser nulo");
    }

    @ParameterizedTest(name = "Formato invalido [{0}] lanza NumeroDeLicenciaInvalidoException")
    @ValueSource(strings = {
            "",
            "   ",
            "123456789",
            "Q1234567",
            "Q123456789",
            "QQ12345678",
            "Q1234A678",
            "Q-12345678",
            "12345678"
    })
    void formatosInvalidosLanzanExcepcion(String valorInvalido) {
        assertThatThrownBy(() -> new NumeroDeLicencia(valorInvalido))
                .isInstanceOf(NumeroDeLicenciaInvalidoException.class)
                .hasMessageContaining("invalido");
    }
}
