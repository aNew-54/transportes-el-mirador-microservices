package pe.edu.unc.elmirador.comercial.models.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.comercial.exceptions.CondicionDePagoInconsistenteException;

class CondicionDePagoTest {

    @Test
    @DisplayName("CondicionDePago: CREDITO con plazo 0 dias es inconsistente y lanza CondicionDePagoInconsistenteException")
    void debeRechazarCreditoConPlazoCero() {
        assertThatThrownBy(() -> new CondicionDePago(ModalidadDePago.CREDITO, 0))
            .isInstanceOf(CondicionDePagoInconsistenteException.class)
            .hasMessageContaining("plazo en dias mayor a cero");
    }

    @Test
    @DisplayName("CondicionDePago: CREDITO con plazo negativo es inconsistente y lanza CondicionDePagoInconsistenteException")
    void debeRechazarCreditoConPlazoNegativo() {
        assertThatThrownBy(() -> new CondicionDePago(ModalidadDePago.CREDITO, -15))
            .isInstanceOf(CondicionDePagoInconsistenteException.class)
            .hasMessageContaining("plazo en dias mayor a cero");
    }

    @Test
    @DisplayName("CondicionDePago: CONTADO con plazo de 30 dias es inconsistente y lanza CondicionDePagoInconsistenteException")
    void debeRechazarContadoConPlazoMayorACero() {
        assertThatThrownBy(() -> new CondicionDePago(ModalidadDePago.CONTADO, 30))
            .isInstanceOf(CondicionDePagoInconsistenteException.class)
            .hasMessageContaining("plazo de exactamente cero dias");
    }

    @Test
    @DisplayName("CondicionDePago: CONTADO con plazo de 0 dias es consistente y esAlContado es true")
    void debeAceptarContadoConPlazoCero() {
        CondicionDePago contado = CondicionDePago.contado();
        assertThat(contado.modalidad()).isEqualTo(ModalidadDePago.CONTADO);
        assertThat(contado.plazoEnDias()).isEqualTo(0);
        assertThat(contado.esAlContado()).isTrue();
        assertThat(contado.esACredito()).isFalse();
    }

    @Test
    @DisplayName("CondicionDePago: CREDITO con plazo positivo es consistente y esACredito es true")
    void debeAceptarCreditoConPlazoPositivo() {
        CondicionDePago credito = CondicionDePago.credito(30);
        assertThat(credito.modalidad()).isEqualTo(ModalidadDePago.CREDITO);
        assertThat(credito.plazoEnDias()).isEqualTo(30);
        assertThat(credito.esACredito()).isTrue();
        assertThat(credito.esAlContado()).isFalse();
    }

    @Test
    @DisplayName("CondicionDePago: Modalidad de pago nula lanza IllegalArgumentException")
    void debeRechazarModalidadNula() {
        assertThatThrownBy(() -> new CondicionDePago(null, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("modalidad de pago es obligatoria");
    }
}
