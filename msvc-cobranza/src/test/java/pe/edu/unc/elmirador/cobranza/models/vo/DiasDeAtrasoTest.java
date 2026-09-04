package pe.edu.unc.elmirador.cobranza.models.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DiasDeAtrasoTest {

    @Test
    @DisplayName("Borde -6: dias menores que -5 corresponden a TramoDeGestion.SIN_ACCION")
    void debeDevolverSinAccionEnMenosSeisDias() {
        DiasDeAtraso atraso = new DiasDeAtraso(-6);
        assertThat(atraso.tramoDeGestion()).isEqualTo(TramoDeGestion.SIN_ACCION);
    }

    @Test
    @DisplayName("Borde -5: dias en -5 corresponden a TramoDeGestion.RECORDATORIO")
    void debeDevolverRecordatorioEnMenosCincoDias() {
        DiasDeAtraso atraso = new DiasDeAtraso(-5);
        assertThat(atraso.tramoDeGestion()).isEqualTo(TramoDeGestion.RECORDATORIO);
    }

    @Test
    @DisplayName("Borde 0: dia del vencimiento (0) corresponde a TramoDeGestion.RECORDATORIO")
    void debeDevolverRecordatorioEnCeroDias() {
        DiasDeAtraso atraso = new DiasDeAtraso(0);
        assertThat(atraso.tramoDeGestion()).isEqualTo(TramoDeGestion.RECORDATORIO);
    }

    @Test
    @DisplayName("Borde 1: primer dia de atraso (1) corresponde a TramoDeGestion.LLAMADA_DE_SEGUIMIENTO")
    void debeDevolverLlamadaDeSeguimientoEnUnDia() {
        DiasDeAtraso atraso = new DiasDeAtraso(1);
        assertThat(atraso.tramoDeGestion()).isEqualTo(TramoDeGestion.LLAMADA_DE_SEGUIMIENTO);
    }

    @Test
    @DisplayName("Borde 15: limite superior (15) corresponde a TramoDeGestion.LLAMADA_DE_SEGUIMIENTO")
    void debeDevolverLlamadaDeSeguimientoEnQuinceDias() {
        DiasDeAtraso atraso = new DiasDeAtraso(15);
        assertThat(atraso.tramoDeGestion()).isEqualTo(TramoDeGestion.LLAMADA_DE_SEGUIMIENTO);
    }

    @Test
    @DisplayName("Borde 16: inicio de tramo (16) corresponde a TramoDeGestion.COMUNICACION_FORMAL")
    void debeDevolverComunicacionFormalEnDieciseisDias() {
        DiasDeAtraso atraso = new DiasDeAtraso(16);
        assertThat(atraso.tramoDeGestion()).isEqualTo(TramoDeGestion.COMUNICACION_FORMAL);
    }

    @Test
    @DisplayName("Borde 30: limite superior (30) corresponde a TramoDeGestion.COMUNICACION_FORMAL")
    void debeDevolverComunicacionFormalEnTreintaDias() {
        DiasDeAtraso atraso = new DiasDeAtraso(30);
        assertThat(atraso.tramoDeGestion()).isEqualTo(TramoDeGestion.COMUNICACION_FORMAL);
    }

    @Test
    @DisplayName("Borde 31: inicio de mora grave (31) corresponde a TramoDeGestion.SUSPENSION_DE_CREDITO")
    void debeDevolverSuspensionDeCreditoEnTreintaYUnDias() {
        DiasDeAtraso atraso = new DiasDeAtraso(31);
        assertThat(atraso.tramoDeGestion()).isEqualTo(TramoDeGestion.SUSPENSION_DE_CREDITO);
    }

    @Test
    @DisplayName("Condicion CCC-01: superaLosTreinta es false en exactamente 30 dias")
    void superaLosTreintaEsFalseEnTreintaDias() {
        DiasDeAtraso atraso = new DiasDeAtraso(30);
        assertThat(atraso.superaLosTreinta()).isFalse();
    }

    @Test
    @DisplayName("Condicion CCC-01: superaLosTreinta es true en 31 dias")
    void superaLosTreintaEsTrueEnTreintaYUnDias() {
        DiasDeAtraso atraso = new DiasDeAtraso(31);
        assertThat(atraso.superaLosTreinta()).isTrue();
    }

    @Test
    @DisplayName("Calcula correctamente los dias entre vencimiento y fecha de referencia")
    void debeCalcularDiasEntreFechasCorrectamente() {
        LocalDate vencimiento = LocalDate.of(2026, 10, 10);
        LocalDate cincoDiasAntes = LocalDate.of(2026, 10, 5);
        LocalDate mismoDia = LocalDate.of(2026, 10, 10);
        LocalDate unDiaDespues = LocalDate.of(2026, 10, 11);
        LocalDate treintaiunDiasDespues = LocalDate.of(2026, 11, 10);

        assertThat(DiasDeAtraso.entre(vencimiento, cincoDiasAntes).dias()).isEqualTo(-5);
        assertThat(DiasDeAtraso.entre(vencimiento, mismoDia).dias()).isEqualTo(0);
        assertThat(DiasDeAtraso.entre(vencimiento, unDiaDespues).dias()).isEqualTo(1);
        assertThat(DiasDeAtraso.entre(vencimiento, treintaiunDiasDespues).dias()).isEqualTo(31);
    }

    @Test
    @DisplayName("Lanza IllegalArgumentException si la fecha de vencimiento es nula")
    void debeRechazarVencimientoNulo() {
        assertThatThrownBy(() -> DiasDeAtraso.entre(null, LocalDate.of(2026, 10, 10)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fecha de vencimiento es obligatoria");
    }

    @Test
    @DisplayName("Lanza IllegalArgumentException si la fecha de referencia es nula")
    void debeRechazarReferenciaNula() {
        assertThatThrownBy(() -> DiasDeAtraso.entre(LocalDate.of(2026, 10, 10), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fecha de referencia es obligatoria");
    }
}
