package pe.edu.unc.elmirador.unidades.models.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.unidades.exceptions.KilometrajeRetrocedeException;

class KilometrajeTest {

    @Test
    @DisplayName("UNI-03: Avanzar a un kilometraje menor lanza KilometrajeRetrocedeException")
    void avanzarAConValorMenorLanzaKilometrajeRetrocedeException_UNI03() {
        // Invariante UNI-03: El kilometraje nunca decrece
        Kilometraje actual = new Kilometraje(50_000);
        Kilometraje menor = new Kilometraje(49_999);

        assertThatThrownBy(() -> actual.avanzarA(menor))
                .isInstanceOf(KilometrajeRetrocedeException.class)
                .hasMessageContaining("UNI-03");
    }

    @Test
    @DisplayName("UNI-03: Avanzar al mismo kilometraje no lanza excepcion y conserva el valor")
    void avanzarAConMismoValorNoLanzaYConservaKilometraje_UNI03() {
        // Invariante UNI-03: No decrece cuando el valor es igual
        Kilometraje actual = new Kilometraje(50_000);
        Kilometraje mismo = new Kilometraje(50_000);

        Kilometraje resultado = actual.avanzarA(mismo);

        assertThat(resultado.valor()).isEqualTo(50_000);
    }

    @Test
    @DisplayName("Avanzar a un kilometraje mayor retorna el nuevo kilometraje")
    void avanzarAConValorMayorRetornaNuevoKilometraje() {
        Kilometraje actual = new Kilometraje(50_000);
        Kilometraje mayor = new Kilometraje(50_500);

        Kilometraje resultado = actual.avanzarA(mayor);

        assertThat(resultado.valor()).isEqualTo(50_500);
    }

    @Test
    @DisplayName("Crear kilometraje con valor negativo lanza IllegalArgumentException")
    void crearConValorNegativoLanzaExcepcion() {
        assertThatThrownBy(() -> new Kilometraje(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no puede ser negativo");
    }

    @Test
    @DisplayName("Avanzar con parametro nulo lanza IllegalArgumentException")
    void avanzarAConNuloLanzaExcepcion() {
        Kilometraje actual = new Kilometraje(10_000);

        assertThatThrownBy(() -> actual.avanzarA(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("compareTo ordena correctamente por valor")
    void compareToOrdenaPorValor() {
        Kilometraje km1 = new Kilometraje(10_000);
        Kilometraje km2 = new Kilometraje(20_000);

        assertThat(km1.compareTo(km2)).isNegative();
        assertThat(km2.compareTo(km1)).isPositive();
        assertThat(km1.compareTo(new Kilometraje(10_000))).isZero();
    }
}
