package pe.edu.unc.elmirador.conductores.models.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.conductores.exceptions.HorasExcedidasException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HorasDeConduccionTest {

    private final PeriodoDeVigencia ventana = new PeriodoDeVigencia(
            LocalDate.of(2026, 9, 10),
            LocalDate.of(2026, 9, 11)
    );

    @Test
    @DisplayName("Crea HorasDeConduccion validas con escala 2")
    void crearHorasDeConduccionValidas() {
        HorasDeConduccion horas = new HorasDeConduccion(new BigDecimal("4.5"), ventana);
        assertThat(horas.horas()).isEqualByComparingTo("4.50");
        assertThat(horas.ventanaDeComputo()).isEqualTo(ventana);
    }

    @Test
    @DisplayName("Horas negativas en constructor lanzan IllegalArgumentException")
    void horasNegativasLanzanExcepcion() {
        assertThatThrownBy(() -> new HorasDeConduccion(new BigDecimal("-1.00"), ventana))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negativas");
    }

    @Test
    @DisplayName("CON-02: Horas mayores al maximo normado (10.00) en constructor lanzan HorasExcedidasException")
    void horasMayoresAlMaximoLanzanExcepcion_CON02() {
        assertThatThrownBy(() -> new HorasDeConduccion(new BigDecimal("10.01"), ventana))
                .as("[CON-02] Horas acumuladas mayores a 10.00 deben lanzar HorasExcedidasException")
                .isInstanceOf(HorasExcedidasException.class)
                .hasMessageContaining("superan el maximo normado");
    }

    @Test
    @DisplayName("CON-02: tieneDisponibles en el borde exacto de MAXIMO_HORAS retorna true")
    void tieneDisponiblesEnBordeExactoRetornaTrue_CON02() {
        HorasDeConduccion horas = new HorasDeConduccion(new BigDecimal("6.00"), ventana);

        // 6.00 + 4.00 = 10.00 (borde exacto de MAXIMO_HORAS)
        assertThat(horas.tieneDisponibles(new BigDecimal("4.00")))
                .as("[CON-02] En el borde exacto de 10.00 debe tener horas disponibles")
                .isTrue();
    }

    @Test
    @DisplayName("CON-02: tieneDisponibles por debajo del maximo retorna true")
    void tieneDisponiblesPorDebajoDelMaximoRetornaTrue_CON02() {
        HorasDeConduccion horas = new HorasDeConduccion(new BigDecimal("6.00"), ventana);

        // 6.00 + 3.99 = 9.99 (por debajo del maximo)
        assertThat(horas.tieneDisponibles(new BigDecimal("3.99")))
                .as("[CON-02] Por debajo del maximo de 10.00 debe tener horas disponibles")
                .isTrue();
    }

    @Test
    @DisplayName("CON-02: tieneDisponibles por encima del maximo retorna false")
    void tieneDisponiblesPorEncimaDelMaximoRetornaFalse_CON02() {
        HorasDeConduccion horas = new HorasDeConduccion(new BigDecimal("6.00"), ventana);

        // 6.00 + 4.01 = 10.01 (por encima del maximo)
        assertThat(horas.tieneDisponibles(new BigDecimal("4.01")))
                .as("[CON-02] Por encima del maximo de 10.00 no debe tener horas disponibles")
                .isFalse();
    }

    @Test
    @DisplayName("tieneDisponibles con parametro nulo o negativo lanza IllegalArgumentException")
    void tieneDisponiblesValidacionesExcepcion() {
        HorasDeConduccion horas = new HorasDeConduccion(new BigDecimal("2.00"), ventana);

        assertThatThrownBy(() -> horas.tieneDisponibles(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> horas.tieneDisponibles(new BigDecimal("-0.50")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("CON-02: acumular en el borde exacto de MAXIMO_HORAS no lanza")
    void acumularEnBordeExactoNoLanza_CON02() {
        HorasDeConduccion horas = new HorasDeConduccion(new BigDecimal("7.00"), ventana);
        HorasDeConduccion resultado = horas.acumular(new BigDecimal("3.00"));

        assertThat(resultado.horas()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("CON-02: acumular superando el maximo normado lanza HorasExcedidasException")
    void acumularSuperandoMaximoLanzaHorasExcedidasException_CON02() {
        HorasDeConduccion horas = new HorasDeConduccion(new BigDecimal("7.00"), ventana);

        // 7.00 + 3.01 = 10.01 > 10.00
        assertThatThrownBy(() -> horas.acumular(new BigDecimal("3.01")))
                .as("[CON-02] Acumular superando 10.00 debe lanzar HorasExcedidasException")
                .isInstanceOf(HorasExcedidasException.class)
                .hasMessageContaining("superarian el maximo normado");
    }

    @Test
    @DisplayName("Calcula correctamente las horas disponibles")
    void calculaHorasDisponiblesCorrectamente() {
        HorasDeConduccion horas = new HorasDeConduccion(new BigDecimal("6.50"), ventana);
        assertThat(horas.disponibles()).isEqualByComparingTo("3.50");
    }
}
