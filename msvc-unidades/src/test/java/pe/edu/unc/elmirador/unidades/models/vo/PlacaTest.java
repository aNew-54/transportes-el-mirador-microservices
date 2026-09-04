package pe.edu.unc.elmirador.unidades.models.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.unidades.exceptions.PlacaInvalidaException;

class PlacaTest {

    @Test
    @DisplayName("Crear placa con formato estandar peruano AAA-000")
    void crearPlacaFormatoEstandarAAA000() {
        Placa placa = new Placa("ABC-123");
        assertThat(placa.valor()).isEqualTo("ABC-123");
    }

    @Test
    @DisplayName("Crear placa con formato especial peruano A0A-000")
    void crearPlacaFormatoEspecialA0A000() {
        Placa placa = new Placa("A1B-123");
        assertThat(placa.valor()).isEqualTo("A1B-123");
    }

    @Test
    @DisplayName("Normaliza la placa a mayusculas y quita espacios en los extremos")
    void crearPlacaNormalizaAMayusculas() {
        Placa placa = new Placa("  abc-123  ");
        assertThat(placa.valor()).isEqualTo("ABC-123");
    }

    @Test
    @DisplayName("Placa con formato invalido lanza PlacaInvalidaException")
    void crearPlacaConFormatoInvalidoLanzaPlacaInvalidaException() {
        assertThatThrownBy(() -> new Placa("123-ABC"))
                .isInstanceOf(PlacaInvalidaException.class);
        assertThatThrownBy(() -> new Placa("ABCD-123"))
                .isInstanceOf(PlacaInvalidaException.class);
        assertThatThrownBy(() -> new Placa("AB-123"))
                .isInstanceOf(PlacaInvalidaException.class);
        assertThatThrownBy(() -> new Placa("ABC-12"))
                .isInstanceOf(PlacaInvalidaException.class);
        assertThatThrownBy(() -> new Placa("ABC123"))
                .isInstanceOf(PlacaInvalidaException.class);
    }

    @Test
    @DisplayName("Placa nula o vacia lanza PlacaInvalidaException")
    void crearPlacaNulaOVaciaLanzaPlacaInvalidaException() {
        assertThatThrownBy(() -> new Placa(null))
                .isInstanceOf(PlacaInvalidaException.class);
        assertThatThrownBy(() -> new Placa(""))
                .isInstanceOf(PlacaInvalidaException.class);
        assertThatThrownBy(() -> new Placa("   "))
                .isInstanceOf(PlacaInvalidaException.class);
    }
}
