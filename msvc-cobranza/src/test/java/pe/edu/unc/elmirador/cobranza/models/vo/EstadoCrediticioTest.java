package pe.edu.unc.elmirador.cobranza.models.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EstadoCrediticioTest {

    private final LocalDate hoy = LocalDate.of(2026, 9, 10);

    @Test
    @DisplayName("Crea estado vigente mediante fabrica y permiteCredito es true")
    void debeCrearEstadoVigente() {
        EstadoCrediticio estado = EstadoCrediticio.vigente(hoy);

        assertThat(estado.situacion()).isEqualTo(SituacionCrediticia.VIGENTE);
        assertThat(estado.motivo()).isNull();
        assertThat(estado.fechaDeCambio()).isEqualTo(hoy);
        assertThat(estado.permiteCredito()).isTrue();
    }

    @Test
    @DisplayName("Lanza IllegalArgumentException si la fecha es nula en fabrica vigente")
    void debeRechazarFechaNulaEnVigente() {
        assertThatThrownBy(() -> EstadoCrediticio.vigente(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fecha de cambio es obligatoria");
    }

    @Test
    @DisplayName("Crea estado suspendido mediante fabrica y permiteCredito es false")
    void debeCrearEstadoSuspendido() {
        EstadoCrediticio estado = EstadoCrediticio.suspendido("Mora grave", hoy);

        assertThat(estado.situacion()).isEqualTo(SituacionCrediticia.SUSPENDIDO);
        assertThat(estado.motivo()).isEqualTo("Mora grave");
        assertThat(estado.fechaDeCambio()).isEqualTo(hoy);
        assertThat(estado.permiteCredito()).isFalse();
    }

    @Test
    @DisplayName("Lanza IllegalArgumentException si motivo es nulo o en blanco en fabrica suspendido")
    void debeRechazarMotivoNuloOEnBlancoEnSuspendido() {
        assertThatThrownBy(() -> EstadoCrediticio.suspendido(null, hoy))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("motivo de suspension es obligatorio");

        assertThatThrownBy(() -> EstadoCrediticio.suspendido("   ", hoy))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("motivo de suspension es obligatorio");
    }

    @Test
    @DisplayName("Lanza IllegalArgumentException si la fecha es nula en fabrica suspendido")
    void debeRechazarFechaNulaEnSuspendido() {
        assertThatThrownBy(() -> EstadoCrediticio.suspendido("Mora", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fecha de cambio es obligatoria");
    }

    @Test
    @DisplayName("Lanza IllegalArgumentException si situacion es nula")
    void debeRechazarSituacionNula() {
        assertThatThrownBy(() -> new EstadoCrediticio(null, "Motivo", hoy))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("situacion crediticia es obligatoria");
    }

    @Test
    @DisplayName("Lanza IllegalArgumentException si fechaDeCambio es nula en constructor directo")
    void debeRechazarFechaNulaEnConstructor() {
        assertThatThrownBy(() -> new EstadoCrediticio(SituacionCrediticia.VIGENTE, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fecha de cambio es obligatoria");
    }
}
