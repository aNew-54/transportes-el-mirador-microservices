package pe.edu.unc.elmirador.ejecucion.models.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EsperaFacturableTest {

    private static final ZoneOffset LIMA = ZoneOffset.ofHours(-5);
    private static final OffsetDateTime T10_00 = OffsetDateTime.of(2026, 9, 10, 10, 0, 0, 0, LIMA);

    @Test
    @DisplayName("[D5] EsperaFacturable.excedente con espera menor al tiempo libre devuelve cero")
    void excedenteConEsperaMenorDevuelveCero() {
        OffsetDateTime fin1Hora = T10_00.plusHours(1);
        EsperaFacturable espera = new EsperaFacturable(T10_00, fin1Hora, 2);

        assertThat(espera.tiempoRealHoras()).isEqualTo(1.0);
        assertThat(espera.excedente()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("[D5] EsperaFacturable.excedente en el borde exacto del tiempo libre devuelve cero")
    void excedenteEnBordeExactoDevuelveCero() {
        OffsetDateTime fin2Horas = T10_00.plusHours(2);
        EsperaFacturable espera = new EsperaFacturable(T10_00, fin2Horas, 2);

        assertThat(espera.tiempoRealHoras()).isEqualTo(2.0);
        assertThat(espera.excedente()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("[D5] EsperaFacturable.excedente con espera mayor al tiempo libre devuelve la diferencia exacta")
    void excedenteConEsperaMayorDevuelveDiferencia() {
        OffsetDateTime fin5Horas = T10_00.plusHours(5);
        EsperaFacturable espera = new EsperaFacturable(T10_00, fin5Horas, 2);

        assertThat(espera.tiempoRealHoras()).isEqualTo(5.0);
        assertThat(espera.excedente()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("[D5] EsperaFacturable.excedente con horas fraccionarias calcula la porcion excedente exacta (contratos 7 y 8)")
    void excedenteConHorasFraccionariasCalculaDiferenciaExacta() {
        // 5 horas y media (330 minutos) con 2 horas libres => 3.5 horas excedentes
        OffsetDateTime fin5HorasMedia = T10_00.plusHours(5).plusMinutes(30);
        EsperaFacturable espera = new EsperaFacturable(T10_00, fin5HorasMedia, 2);

        assertThat(espera.tiempoRealHoras()).isEqualTo(5.5);
        assertThat(espera.excedente()).isEqualTo(3.5);
    }

    @Test
    @DisplayName("Constructor con fecha de fin anterior o igual a inicio lanza IllegalArgumentException")
    void finAnteriorOIgualAInicioLanzaExcepcion() {
        assertThatThrownBy(() -> new EsperaFacturable(T10_00, T10_00, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("posterior");

        assertThatThrownBy(() -> new EsperaFacturable(T10_00, T10_00.minusHours(1), 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("posterior");
    }

    @Test
    @DisplayName("Constructor con fechas nulas lanza IllegalArgumentException")
    void fechasNulasLanzanExcepcion() {
        assertThatThrownBy(() -> new EsperaFacturable(null, T10_00.plusHours(1), 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EsperaFacturable(T10_00, null, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Constructor con tiempo libre negativo lanza IllegalArgumentException")
    void tiempoLibreNegativoLanzaExcepcion() {
        assertThatThrownBy(() -> new EsperaFacturable(T10_00, T10_00.plusHours(1), -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negativo");
    }
}
