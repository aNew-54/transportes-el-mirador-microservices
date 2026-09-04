package pe.edu.unc.elmirador.cobranza.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.cobranza.exceptions.DominioCobranzaException;
import pe.edu.unc.elmirador.cobranza.exceptions.RehabilitacionInvalidaException;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaCorrienteDelCliente;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaPorCobrar;
import pe.edu.unc.elmirador.cobranza.models.vo.Dinero;
import pe.edu.unc.elmirador.cobranza.models.vo.EstadoCrediticio;
import pe.edu.unc.elmirador.cobranza.models.vo.SituacionCrediticia;

class CuentaCorrienteDelClienteTest {

    private final String clienteId = "CLI-0007";
    private final LocalDate hoy = LocalDate.of(2026, 9, 10);

    @Test
    @DisplayName("CCC-01: En exactamente 30 dias de atraso el cliente NO se suspende (permanece VIGENTE)")
    void ccc01_noSeSuspendeEnTreintaDiasExactos() {
        CuentaCorrienteDelCliente ccc = new CuentaCorrienteDelCliente(
            clienteId, EstadoCrediticio.vigente(hoy.minusDays(60))
        );

        // Vencimiento hace exactamente 30 dias: hoy - 30
        LocalDate vencimiento = hoy.minusDays(30);
        CuentaPorCobrar cuenta = new CuentaPorCobrar(
            "CPC-001", clienteId, "FAC-001", "F001-001",
            Dinero.de("1000.00", "PEN"), Dinero.cero("PEN"), Dinero.de("1000.00", "PEN"), vencimiento
        );
        ccc.registrarCuenta(cuenta);

        ccc.evaluarCredito(hoy);

        assertThat(ccc.estado().situacion()).isEqualTo(SituacionCrediticia.VIGENTE);
        assertThat(ccc.estado().permiteCredito()).isTrue();
    }

    @Test
    @DisplayName("CCC-01: Al cruzar los 30 dias de atraso (31 dias) el cliente queda SUSPENDIDO automaticamente")
    void ccc01_suspendeAutomaticamenteAlSuperarTreintaDias() {
        CuentaCorrienteDelCliente ccc = new CuentaCorrienteDelCliente(
            clienteId, EstadoCrediticio.vigente(hoy.minusDays(60))
        );

        // Vencimiento hace 31 dias: hoy - 31
        LocalDate vencimiento = hoy.minusDays(31);
        CuentaPorCobrar cuenta = new CuentaPorCobrar(
            "CPC-001", clienteId, "FAC-001", "F001-001",
            Dinero.de("1000.00", "PEN"), Dinero.cero("PEN"), Dinero.de("1000.00", "PEN"), vencimiento
        );
        ccc.registrarCuenta(cuenta);

        ccc.evaluarCredito(hoy);

        assertThat(ccc.estado().situacion()).isEqualTo(SituacionCrediticia.SUSPENDIDO);
        assertThat(ccc.estado().permiteCredito()).isFalse();
        assertThat(ccc.estado().motivo()).contains("superior a 30 dias");
        assertThat(ccc.estado().fechaDeCambio()).isEqualTo(hoy);
    }

    @Test
    @DisplayName("CCC-01: Cuentas canceladas no provocan suspension aunque su vencimiento supere 30 dias")
    void ccc01_cuentasCanceladasNoProvocanSuspension() {
        CuentaCorrienteDelCliente ccc = new CuentaCorrienteDelCliente(
            clienteId, EstadoCrediticio.vigente(hoy.minusDays(60))
        );

        LocalDate vencimiento = hoy.minusDays(45);
        CuentaPorCobrar cuenta = new CuentaPorCobrar(
            "CPC-001", clienteId, "FAC-001", "F001-001",
            Dinero.de("1000.00", "PEN"), Dinero.cero("PEN"), Dinero.de("1000.00", "PEN"), vencimiento
        );
        cuenta.aplicar(Dinero.de("1000.00", "PEN")); // Totalmente cancelada
        ccc.registrarCuenta(cuenta);

        ccc.evaluarCredito(hoy);

        assertThat(ccc.estado().situacion()).isEqualTo(SituacionCrediticia.VIGENTE);
    }

    @Test
    @DisplayName("Rehabilitar credito sobre una cartera con una cuenta de 45 dias lanza RehabilitacionInvalidaException")
    void debeFallarAlRehabilitarConCuentaDe45DiasDeAtraso() {
        CuentaCorrienteDelCliente ccc = new CuentaCorrienteDelCliente(
            clienteId, EstadoCrediticio.suspendido("Mora previa", hoy.minusDays(10))
        );

        LocalDate vencimiento = hoy.minusDays(45);
        CuentaPorCobrar cuenta = new CuentaPorCobrar(
            "CPC-001", clienteId, "FAC-001", "F001-001",
            Dinero.de("1000.00", "PEN"), Dinero.cero("PEN"), Dinero.de("1000.00", "PEN"), vencimiento
        );
        ccc.registrarCuenta(cuenta);

        assertThatThrownBy(() -> ccc.rehabilitarCredito(hoy))
            .isInstanceOf(RehabilitacionInvalidaException.class)
            .hasMessageContaining("mas de 30 dias de atraso");

        assertThat(ccc.estado().situacion()).isEqualTo(SituacionCrediticia.SUSPENDIDO);
    }

    @Test
    @DisplayName("Rehabilitar credito tiene exito si la cuenta atrasada fue cancelada o regularizada")
    void debeRehabilitarCreditoSiCuentasEstanRegularizadas() {
        CuentaCorrienteDelCliente ccc = new CuentaCorrienteDelCliente(
            clienteId, EstadoCrediticio.suspendido("Mora regularizada", hoy.minusDays(5))
        );

        LocalDate vencimiento = hoy.minusDays(45);
        CuentaPorCobrar cuenta = new CuentaPorCobrar(
            "CPC-001", clienteId, "FAC-001", "F001-001",
            Dinero.de("1000.00", "PEN"), Dinero.cero("PEN"), Dinero.de("1000.00", "PEN"), vencimiento
        );
        cuenta.aplicar(Dinero.de("1000.00", "PEN"));
        ccc.registrarCuenta(cuenta);

        ccc.rehabilitarCredito(hoy);

        assertThat(ccc.estado().situacion()).isEqualTo(SituacionCrediticia.VIGENTE);
        assertThat(ccc.estado().permiteCredito()).isTrue();
        assertThat(ccc.estado().fechaDeCambio()).isEqualTo(hoy);
    }

    @Test
    @DisplayName("Suspension manual de credito actualiza estado a SUSPENDIDO con motivo y fecha")
    void debeSuspenderCreditoManualmente() {
        CuentaCorrienteDelCliente ccc = new CuentaCorrienteDelCliente(
            clienteId, EstadoCrediticio.vigente(hoy.minusDays(30))
        );

        ccc.suspenderCredito("Decision gerencial por riesgo financiero", hoy);

        assertThat(ccc.estado().situacion()).isEqualTo(SituacionCrediticia.SUSPENDIDO);
        assertThat(ccc.estado().motivo()).isEqualTo("Decision gerencial por riesgo financiero");
    }

    @Test
    @DisplayName("Registrar cuenta rechaza cuenta de otro cliente lanzando DominioCobranzaException")
    void debeRechazarCuentaDeOtroCliente() {
        CuentaCorrienteDelCliente ccc = new CuentaCorrienteDelCliente(
            clienteId, EstadoCrediticio.vigente(hoy)
        );

        CuentaPorCobrar cuentaOtro = new CuentaPorCobrar(
            "CPC-001", "CLI-9999", "FAC-001", "F001-001",
            Dinero.de("1000.00", "PEN"), Dinero.cero("PEN"), Dinero.de("1000.00", "PEN"), hoy
        );

        assertThatThrownBy(() -> ccc.registrarCuenta(cuentaOtro))
            .isInstanceOf(DominioCobranzaException.class)
            .hasMessageContaining("CLI-9999")
            .hasMessageContaining(clienteId);
    }

    @Test
    @DisplayName("Registrar cuenta rechaza facturaId ya registrada lanzando DominioCobranzaException")
    void debeRechazarFacturaDuplicada() {
        CuentaCorrienteDelCliente ccc = new CuentaCorrienteDelCliente(
            clienteId, EstadoCrediticio.vigente(hoy)
        );

        CuentaPorCobrar c1 = new CuentaPorCobrar(
            "CPC-001", clienteId, "FAC-001", "F001-001",
            Dinero.de("1000.00", "PEN"), Dinero.cero("PEN"), Dinero.de("1000.00", "PEN"), hoy
        );
        CuentaPorCobrar c2 = new CuentaPorCobrar(
            "CPC-002", clienteId, "FAC-001", "F001-002",
            Dinero.de("2000.00", "PEN"), Dinero.cero("PEN"), Dinero.de("2000.00", "PEN"), hoy
        );

        ccc.registrarCuenta(c1);

        assertThatThrownBy(() -> ccc.registrarCuenta(c2))
            .isInstanceOf(DominioCobranzaException.class)
            .hasMessageContaining("ya se encuentra registrada");
    }

    @Test
    @DisplayName("deudaTotal suma saldos de cuentas no canceladas")
    void debeCalcularDeudaTotal() {
        CuentaCorrienteDelCliente ccc = new CuentaCorrienteDelCliente(
            clienteId, EstadoCrediticio.vigente(hoy)
        );

        CuentaPorCobrar c1 = new CuentaPorCobrar(
            "CPC-001", clienteId, "FAC-001", "F001-001",
            Dinero.de("1000.00", "PEN"), Dinero.cero("PEN"), Dinero.de("1000.00", "PEN"), hoy
        );
        c1.aplicar(Dinero.de("300.00", "PEN")); // Saldo 700.00

        CuentaPorCobrar c2 = new CuentaPorCobrar(
            "CPC-002", clienteId, "FAC-002", "F001-002",
            Dinero.de("500.00", "PEN"), Dinero.cero("PEN"), Dinero.de("500.00", "PEN"), hoy
        ); // Saldo 500.00

        CuentaPorCobrar c3 = new CuentaPorCobrar(
            "CPC-003", clienteId, "FAC-003", "F001-003",
            Dinero.de("800.00", "PEN"), Dinero.cero("PEN"), Dinero.de("800.00", "PEN"), hoy
        );
        c3.aplicar(Dinero.de("800.00", "PEN")); // Cancelada, saldo 0.00

        ccc.registrarCuenta(c1);
        ccc.registrarCuenta(c2);
        ccc.registrarCuenta(c3);

        assertThat(ccc.deudaTotal("PEN")).isEqualTo(Dinero.de("1200.00", "PEN"));
    }

    @Test
    @DisplayName("Alimentacion Contrato 11: diasDeAtrasoMaximo y cuentasVencidas calculan correctamente")
    void debeCalcularMetricasParaContrato11() {
        CuentaCorrienteDelCliente ccc = new CuentaCorrienteDelCliente(
            clienteId, EstadoCrediticio.vigente(hoy)
        );

        // Cuenta 1: vencio hace 43 dias
        CuentaPorCobrar c1 = new CuentaPorCobrar(
            "CPC-001", clienteId, "FAC-001", "F001-001",
            Dinero.de("3000.00", "PEN"), Dinero.cero("PEN"), Dinero.de("3000.00", "PEN"), hoy.minusDays(43)
        );

        // Cuenta 2: vencio hace 12 dias
        CuentaPorCobrar c2 = new CuentaPorCobrar(
            "CPC-002", clienteId, "FAC-002", "F001-002",
            Dinero.de("2420.30", "PEN"), Dinero.cero("PEN"), Dinero.de("2420.30", "PEN"), hoy.minusDays(12)
        );

        // Cuenta 3: aun no vence (vence en 10 dias)
        CuentaPorCobrar c3 = new CuentaPorCobrar(
            "CPC-003", clienteId, "FAC-003", "F001-003",
            Dinero.de("1500.00", "PEN"), Dinero.cero("PEN"), Dinero.de("1500.00", "PEN"), hoy.plusDays(10)
        );

        // Cuenta 4: vencio hace 60 dias pero esta cancelada
        CuentaPorCobrar c4 = new CuentaPorCobrar(
            "CPC-004", clienteId, "FAC-004", "F001-004",
            Dinero.de("900.00", "PEN"), Dinero.cero("PEN"), Dinero.de("900.00", "PEN"), hoy.minusDays(60)
        );
        c4.aplicar(Dinero.de("900.00", "PEN"));

        ccc.registrarCuenta(c1);
        ccc.registrarCuenta(c2);
        ccc.registrarCuenta(c3);
        ccc.registrarCuenta(c4);

        assertThat(ccc.diasDeAtrasoMaximo(hoy)).isEqualTo(43);
        assertThat(ccc.cuentasVencidas(hoy)).isEqualTo(2);
        assertThat(ccc.deudaTotal("PEN")).isEqualTo(Dinero.de("6920.30", "PEN"));
    }

    @Test
    @DisplayName("Metodos con dependencia temporal lanzan IllegalArgumentException si la fecha es nula")
    void debeRechazarFechasNulas() {
        CuentaCorrienteDelCliente ccc = new CuentaCorrienteDelCliente(
            clienteId, EstadoCrediticio.vigente(hoy)
        );

        assertThatThrownBy(() -> ccc.evaluarCredito(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fecha es obligatoria");

        assertThatThrownBy(() -> ccc.rehabilitarCredito(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fecha es obligatoria");

        assertThatThrownBy(() -> ccc.suspenderCredito("Motivo", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fecha es obligatoria");

        assertThatThrownBy(() -> ccc.diasDeAtrasoMaximo(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fecha es obligatoria");

        assertThatThrownBy(() -> ccc.cuentasVencidas(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fecha es obligatoria");
    }
}
