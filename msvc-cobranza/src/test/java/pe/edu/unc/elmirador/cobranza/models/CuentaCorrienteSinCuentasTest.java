package pe.edu.unc.elmirador.cobranza.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaCorrienteDelCliente;
import pe.edu.unc.elmirador.cobranza.models.vo.Dinero;
import pe.edu.unc.elmirador.cobranza.models.vo.EstadoCrediticio;

/**
 * Un cliente sin cartera es un caso normal, no un error.
 *
 * <p>Es lo primero que devuelve el contrato 11 para un cliente nuevo, y la version anterior de
 * deudaTotal() lanzaba IllegalStateException al intentar deducir la moneda de la primera cuenta.
 */
class CuentaCorrienteSinCuentasTest {

    private final LocalDate hoy = LocalDate.of(2026, 9, 4);

    private CuentaCorrienteDelCliente sinCuentas() {
        return new CuentaCorrienteDelCliente("CLI-0099", EstadoCrediticio.vigente(hoy));
    }

    @Test
    @DisplayName("un cliente sin cuentas responde deuda cero, no un error")
    void deudaCeroSinCuentas() {
        CuentaCorrienteDelCliente ccc = sinCuentas();

        assertThat(ccc.deudaTotal("PEN")).isEqualTo(Dinero.cero("PEN"));
        assertThat(ccc.deudaTotal("USD")).isEqualTo(Dinero.cero("USD"));
        assertThat(ccc.diasDeAtrasoMaximo(hoy)).isZero();
        assertThat(ccc.cuentasVencidas(hoy)).isZero();
    }

    @Test
    @DisplayName("CCC-01: sin cuentas no hay nada que suspender")
    void evaluarCreditoSinCuentasNoSuspende() {
        CuentaCorrienteDelCliente ccc = sinCuentas();

        ccc.evaluarCredito(hoy);

        assertThat(ccc.estado().permiteCredito())
                .as("[CCC-01] la suspension exige una cuenta con mas de 30 dias de atraso")
                .isTrue();
    }

    @Test
    @DisplayName("la moneda de la deuda es obligatoria: no se adivina")
    void deudaTotalExigeMoneda() {
        CuentaCorrienteDelCliente ccc = sinCuentas();

        assertThatThrownBy(() -> ccc.deudaTotal(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ccc.deudaTotal("  ")).isInstanceOf(IllegalArgumentException.class);
    }
}
