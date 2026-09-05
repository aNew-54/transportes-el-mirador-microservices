package pe.edu.unc.elmirador.ejecucion.models.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.ejecucion.exceptions.DominioEjecucionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EstadoConformidadTest {

    @Test
    @DisplayName("codigoDelContrato: FIRMADA devuelve FIRMADA")
    void codigoFirmada() {
        assertThat(EstadoConformidad.FIRMADA.codigoDelContrato()).isEqualTo("FIRMADA");
    }

    @Test
    @DisplayName("codigoDelContrato: OBSERVADA devuelve PARCIAL")
    void codigoObservada() {
        assertThat(EstadoConformidad.OBSERVADA.codigoDelContrato()).isEqualTo("PARCIAL");
    }

    @Test
    @DisplayName("codigoDelContrato: PENDIENTE lanza DominioEjecucionException")
    void codigoPendiente() {
        assertThatThrownBy(() -> EstadoConformidad.PENDIENTE.codigoDelContrato())
                .isInstanceOf(DominioEjecucionException.class);
    }
}
