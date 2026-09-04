package pe.edu.unc.elmirador.conductores.models.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PeriodoDeVigenciaTest {

    @Test
    @DisplayName("Crea periodo valido con fecha hasta posterior a desde")
    void crearPeriodoValido() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 12, 31);
        PeriodoDeVigencia periodo = new PeriodoDeVigencia(desde, hasta);

        assertThat(periodo.desde()).isEqualTo(desde);
        assertThat(periodo.hasta()).isEqualTo(hasta);
    }

    @Test
    @DisplayName("Fecha hasta igual a desde lanza IllegalArgumentException")
    void hastaNoPosteriorADesdeLanzaExcepcion() {
        LocalDate fecha = LocalDate.of(2026, 6, 1);
        assertThatThrownBy(() -> new PeriodoDeVigencia(fecha, fecha))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("posterior");
    }

    @Test
    @DisplayName("Fecha hasta anterior a desde lanza IllegalArgumentException")
    void hastaAnteriorADesdeLanzaExcepcion() {
        LocalDate desde = LocalDate.of(2026, 6, 10);
        LocalDate hasta = LocalDate.of(2026, 6, 1);
        assertThatThrownBy(() -> new PeriodoDeVigencia(desde, hasta))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("posterior");
    }

    @Test
    @DisplayName("Fechas nulas en constructor lanzan IllegalArgumentException")
    void fechasNulasLanzanExcepcion() {
        LocalDate fecha = LocalDate.of(2026, 6, 1);
        assertThatThrownBy(() -> new PeriodoDeVigencia(null, fecha))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PeriodoDeVigencia(fecha, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("estaVigenteEn retorna true en los extremos inclusive y dentro del intervalo")
    void estaVigenteEnIntervaloInclusive() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 12, 31);
        PeriodoDeVigencia periodo = new PeriodoDeVigencia(desde, hasta);

        assertThat(periodo.estaVigenteEn(desde)).isTrue();
        assertThat(periodo.estaVigenteEn(hasta)).isTrue();
        assertThat(periodo.estaVigenteEn(LocalDate.of(2026, 6, 15))).isTrue();
    }

    @Test
    @DisplayName("estaVigenteEn retorna false antes de la fecha desde y despues de hasta")
    void noEstaVigenteFueraDelIntervalo() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 12, 31);
        PeriodoDeVigencia periodo = new PeriodoDeVigencia(desde, hasta);

        assertThat(periodo.estaVigenteEn(desde.minusDays(1))).isFalse();
        assertThat(periodo.estaVigenteEn(hasta.plusDays(1))).isFalse();
    }

    @Test
    @DisplayName("estaVigenteEn con fecha nula lanza IllegalArgumentException (el dominio no lee el reloj)")
    void estaVigenteEnFechaNulaLanzaExcepcion() {
        PeriodoDeVigencia periodo = new PeriodoDeVigencia(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)
        );
        assertThatThrownBy(() -> periodo.estaVigenteEn(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no puede ser nula");
    }

    @Test
    @DisplayName("venceDentroDe dias retorna true cuando vence en el intervalo indicado")
    void venceDentroDeDiasRetornaTrueCuandoVenceEnElIntervalo() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 12, 31);
        PeriodoDeVigencia periodo = new PeriodoDeVigencia(desde, hasta);

        LocalDate ref = LocalDate.of(2026, 12, 25);
        assertThat(periodo.venceDentroDe(10, ref)).isTrue();
    }

    @Test
    @DisplayName("venceDentroDe retorna false si el documento ya vencio en la fecha de referencia")
    void venceDentroDeRetornaFalseSiYaVencio() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 12, 31);
        PeriodoDeVigencia periodo = new PeriodoDeVigencia(desde, hasta);

        LocalDate ref = LocalDate.of(2027, 1, 5);
        assertThat(periodo.venceDentroDe(30, ref)).isFalse();
    }

    @Test
    @DisplayName("venceDentroDe retorna false si vence mas alla del plazo indicado")
    void venceDentroDeRetornaFalseSiVenceMasAllaDelIntervalo() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 12, 31);
        PeriodoDeVigencia periodo = new PeriodoDeVigencia(desde, hasta);

        LocalDate ref = LocalDate.of(2026, 6, 1);
        assertThat(periodo.venceDentroDe(15, ref)).isFalse();
    }

    @Test
    @DisplayName("venceDentroDe con fecha de referencia nula o dias negativos lanza IllegalArgumentException")
    void venceDentroDeValidacionesExcepcion() {
        PeriodoDeVigencia periodo = new PeriodoDeVigencia(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)
        );
        LocalDate ref = LocalDate.of(2026, 6, 1);

        assertThatThrownBy(() -> periodo.venceDentroDe(10, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> periodo.venceDentroDe(-1, ref))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
