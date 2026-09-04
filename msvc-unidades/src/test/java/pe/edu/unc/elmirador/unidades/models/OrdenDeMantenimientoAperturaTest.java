package pe.edu.unc.elmirador.unidades.models;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.unidades.models.entity.OrdenDeMantenimiento;
import pe.edu.unc.elmirador.unidades.models.vo.Kilometraje;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeMantenimiento;

/**
 * Apertura de la orden. Una invariante que se puede saltar pasando null no es una invariante.
 */
class OrdenDeMantenimientoAperturaTest {

    private final LocalDate hoy = LocalDate.of(2026, 9, 4);

    @Test
    @DisplayName("OMT-02: abrir sin el kilometraje del ultimo mantenimiento lanza, no pasa de largo")
    void sinUltimoMantenimientoNoSeEvaluaOmt02() {
        assertThatThrownBy(() -> OrdenDeMantenimiento.abrir(
                        "OMT-900",
                        "UNI-001",
                        TipoDeMantenimiento.PREVENTIVO,
                        new Kilometraje(10_000),
                        null,
                        hoy,
                        "PEN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OMT-02");
    }

    @Test
    @DisplayName("la orden exige moneda explicita: no se adivina la moneda de un importe")
    void sinMonedaNoHayOrden() {
        assertThatThrownBy(() -> OrdenDeMantenimiento.abrir(
                        "OMT-901",
                        "UNI-001",
                        TipoDeMantenimiento.CORRECTIVO,
                        new Kilometraje(10_000),
                        new Kilometraje(5_000),
                        hoy,
                        "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("moneda");
    }
}
