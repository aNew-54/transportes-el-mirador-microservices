package pe.edu.unc.elmirador.unidades.models.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PeriodoDeVigenciaTest {

    @Test
    @DisplayName("Crear periodo valido con 'hasta' posterior a 'desde'")
    void crearPeriodoValido() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 12, 31);

        PeriodoDeVigencia periodo = new PeriodoDeVigencia(desde, hasta);

        assertThat(periodo.desde()).isEqualTo(desde);
        assertThat(periodo.hasta()).isEqualTo(hasta);
    }

    @Test
    @DisplayName("Crear periodo con 'hasta' anterior o igual a 'desde' lanza IllegalArgumentException")
    void hastaNoPosteriorADesdeLanzaExcepcion() {
        LocalDate fecha = LocalDate.of(2026, 6, 15);

        // hasta igual a desde
        assertThatThrownBy(() -> new PeriodoDeVigencia(fecha, fecha))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("posterior a 'desde'");

        // hasta anterior a desde
        assertThatThrownBy(() -> new PeriodoDeVigencia(fecha, fecha.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("posterior a 'desde'");
    }

    @Test
    @DisplayName("estaVigenteEn retorna true dentro del intervalo inclusive")
    void estaVigenteEnIntervaloInclusive() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 12, 31);
        PeriodoDeVigencia periodo = new PeriodoDeVigencia(desde, hasta);

        assertThat(periodo.estaVigenteEn(desde)).isTrue();
        assertThat(periodo.estaVigenteEn(hasta)).isTrue();
        assertThat(periodo.estaVigenteEn(LocalDate.of(2026, 6, 15))).isTrue();

        assertThat(periodo.estaVigenteEn(desde.minusDays(1))).isFalse();
        assertThat(periodo.estaVigenteEn(hasta.plusDays(1))).isFalse();
    }

    @Test
    @DisplayName("venceDentroDe retorna true si 'hasta' cae dentro de los N dias desde la fecha de referencia")
    void venceDentroDeDiasRetornaTrueCuandoVenceEnElIntervalo() {
        LocalDate ref = LocalDate.of(2026, 9, 1);
        PeriodoDeVigencia periodo = new PeriodoDeVigencia(
                LocalDate.of(2025, 9, 1),
                LocalDate.of(2026, 9, 10)); // vence en 9 dias

        // Dentro de 10 dias vence
        assertThat(periodo.venceDentroDe(10, ref)).isTrue();
        // Dentro de 9 dias vence (exacto)
        assertThat(periodo.venceDentroDe(9, ref)).isTrue();
        // Dentro de 8 dias todavia no vence
        assertThat(periodo.venceDentroDe(8, ref)).isFalse();
    }

    @Test
    @DisplayName("venceDentroDe retorna false si ya vencio antes de la fecha de referencia")
    void venceDentroDeRetornaFalseSiYaVencio() {
        LocalDate ref = LocalDate.of(2026, 9, 1);
        PeriodoDeVigencia periodo = new PeriodoDeVigencia(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2026, 8, 31)); // vencio ayer

        assertThat(periodo.venceDentroDe(15, ref)).isFalse();
    }
}
