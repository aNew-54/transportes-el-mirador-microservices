package pe.edu.unc.elmirador.cobranza.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.cobranza.exceptions.AplicacionExcedeElPagoException;
import pe.edu.unc.elmirador.cobranza.exceptions.MonedaIncompatibleException;
import pe.edu.unc.elmirador.cobranza.exceptions.PagoDeOtroClienteException;
import pe.edu.unc.elmirador.cobranza.exceptions.SaldoInsuficienteException;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaPorCobrar;
import pe.edu.unc.elmirador.cobranza.models.entity.Pago;
import pe.edu.unc.elmirador.cobranza.models.vo.Dinero;
import pe.edu.unc.elmirador.cobranza.models.vo.MedioDePago;

class PagoTest {

    private final String clienteId = "CLI-0007";
    private final LocalDate hoy = LocalDate.of(2026, 9, 10);
    private final LocalDate vencimiento = LocalDate.of(2026, 10, 10);

    @Test
    @DisplayName("PAG-01: La suma de aplicaciones no puede exceder el monto del pago; el intento fallido no altera ni el pago ni la cuenta")
    void pag01_debeRechazarAplicacionQueExcedeMontoDelPagoYSinAlterarEstado() {
        Pago pago = new Pago(
            "PAG-001", clienteId, Dinero.de("1000.00", "PEN"),
            MedioDePago.transferencia("TX-998877"), hoy
        );

        CuentaPorCobrar cpc = new CuentaPorCobrar(
            "CPC-001", clienteId, "FAC-001", "F001-001",
            Dinero.de("2000.00", "PEN"), Dinero.cero("PEN"), vencimiento
        );

        // Primera aplicacion valida de 800.00 (queda saldo sin aplicar = 200.00)
        pago.aplicarACuentaPorCobrar(cpc, Dinero.de("800.00", "PEN"));
        assertThat(pago.montoAplicado()).isEqualTo(Dinero.de("800.00", "PEN"));
        assertThat(pago.saldoSinAplicar()).isEqualTo(Dinero.de("200.00", "PEN"));
        assertThat(cpc.aplicado()).isEqualTo(Dinero.de("800.00", "PEN"));

        // Intento fallido de aplicar 200.01 (excede saldo sin aplicar del pago)
        Dinero aplicadoPagoAntes = pago.montoAplicado();
        Dinero saldoPagoAntes = pago.saldoSinAplicar();
        Dinero aplicadoCuentaAntes = cpc.aplicado();
        int cantAplicacionesAntes = pago.aplicaciones().size();

        assertThatThrownBy(() -> pago.aplicarACuentaPorCobrar(cpc, Dinero.de("200.01", "PEN")))
            .isInstanceOf(AplicacionExcedeElPagoException.class)
            .hasMessageContaining("excede el saldo sin aplicar del pago");

        // Verificamos que ni el pago ni la cuenta fueron alterados
        assertThat(pago.montoAplicado()).isEqualTo(aplicadoPagoAntes);
        assertThat(pago.saldoSinAplicar()).isEqualTo(saldoPagoAntes);
        assertThat(pago.aplicaciones()).hasSize(cantAplicacionesAntes);
        assertThat(cpc.aplicado()).isEqualTo(aplicadoCuentaAntes);
    }

    @Test
    @DisplayName("PAG-01: Aplicar exactamente el saldo total del pago deja saldoSinAplicar en cero")
    void pag01_debePermitirAplicarTotalExactoDelPago() {
        Pago pago = new Pago(
            "PAG-001", clienteId, Dinero.de("1000.00", "PEN"),
            MedioDePago.efectivo(), hoy
        );

        CuentaPorCobrar cpc = new CuentaPorCobrar(
            "CPC-001", clienteId, "FAC-001", "F001-001",
            Dinero.de("1500.00", "PEN"), Dinero.cero("PEN"), vencimiento
        );

        pago.aplicarACuentaPorCobrar(cpc, Dinero.de("1000.00", "PEN"));

        assertThat(pago.montoAplicado()).isEqualTo(Dinero.de("1000.00", "PEN"));
        assertThat(pago.saldoSinAplicar().esCero()).isTrue();
        assertThat(cpc.aplicado()).isEqualTo(Dinero.de("1000.00", "PEN"));
        assertThat(pago.aplicaciones()).hasSize(1);
    }

    @Test
    @DisplayName("PAG-02: Aplicar a la cuenta de otro cliente lanza PagoDeOtroClienteException y no altera nada")
    void pag02_debeRechazarAplicacionACuentaDeOtroClienteYSinAlterarEstado() {
        Pago pago = new Pago(
            "PAG-001", clienteId, Dinero.de("1000.00", "PEN"),
            MedioDePago.deposito("DEP-112233"), hoy
        );

        CuentaPorCobrar cuentaDeOtro = new CuentaPorCobrar(
            "CPC-999", "CLI-9999", "FAC-999", "F001-999",
            Dinero.de("500.00", "PEN"), Dinero.cero("PEN"), vencimiento
        );

        assertThatThrownBy(() -> pago.aplicarACuentaPorCobrar(cuentaDeOtro, Dinero.de("500.00", "PEN")))
            .isInstanceOf(PagoDeOtroClienteException.class)
            .hasMessageContaining("cuenta pertenece al cliente CLI-9999");

        assertThat(pago.montoAplicado().esCero()).isTrue();
        assertThat(pago.aplicaciones()).isEmpty();
        assertThat(cuentaDeOtro.aplicado().esCero()).isTrue();
    }

    @Test
    @DisplayName("Un pago cubre dos facturas mediante aplicaciones sucesivas")
    void debePermitirUnPagoQueCubreDosFacturas() {
        Pago pago = new Pago(
            "PAG-001", clienteId, Dinero.de("1500.00", "PEN"),
            MedioDePago.transferencia("OP-554433"), hoy
        );

        CuentaPorCobrar cpc1 = new CuentaPorCobrar(
            "CPC-001", clienteId, "FAC-001", "F001-001",
            Dinero.de("800.00", "PEN"), Dinero.cero("PEN"), vencimiento
        );

        CuentaPorCobrar cpc2 = new CuentaPorCobrar(
            "CPC-002", clienteId, "FAC-002", "F001-002",
            Dinero.de("700.00", "PEN"), Dinero.cero("PEN"), vencimiento
        );

        pago.aplicarACuentaPorCobrar(cpc1, Dinero.de("800.00", "PEN"));
        pago.aplicarACuentaPorCobrar(cpc2, Dinero.de("700.00", "PEN"));

        assertThat(pago.montoAplicado()).isEqualTo(Dinero.de("1500.00", "PEN"));
        assertThat(pago.saldoSinAplicar().esCero()).isTrue();
        assertThat(pago.aplicaciones()).hasSize(2);

        assertThat(cpc1.saldo().esCero()).isTrue();
        assertThat(cpc1.estaCancelada()).isTrue();

        assertThat(cpc2.saldo().esCero()).isTrue();
        assertThat(cpc2.estaCancelada()).isTrue();
    }

    @Test
    @DisplayName("Una factura cobrada en dos pagos sucesivos de clientes regulariza la cuenta")
    void debePermitirUnaFacturaCobradaEnDosPagos() {
        CuentaPorCobrar cpc = new CuentaPorCobrar(
            "CPC-001", clienteId, "FAC-001", "F001-001",
            Dinero.de("1000.00", "PEN"), Dinero.cero("PEN"), vencimiento
        );

        Pago pago1 = new Pago(
            "PAG-001", clienteId, Dinero.de("600.00", "PEN"),
            MedioDePago.transferencia("TX-001"), hoy
        );

        Pago pago2 = new Pago(
            "PAG-002", clienteId, Dinero.de("400.00", "PEN"),
            MedioDePago.cheque("CHQ-002"), hoy
        );

        pago1.aplicarACuentaPorCobrar(cpc, Dinero.de("600.00", "PEN"));
        assertThat(cpc.saldo()).isEqualTo(Dinero.de("400.00", "PEN"));
        assertThat(cpc.estaCancelada()).isFalse();

        pago2.aplicarACuentaPorCobrar(cpc, Dinero.de("400.00", "PEN"));
        assertThat(cpc.saldo().esCero()).isTrue();
        assertThat(cpc.estaCancelada()).isTrue();
    }

    @Test
    @DisplayName("Aplicar a cuenta con saldo menor que el importe lanza SaldoInsuficienteException y NO altera ni el pago ni la cuenta")
    void debeRechazarAplicacionQueExcedeSaldoDeLaCuentaYSinAlterarEstado() {
        Pago pago = new Pago(
            "PAG-001", clienteId, Dinero.de("1000.00", "PEN"),
            MedioDePago.transferencia("TX-999"), hoy
        );

        CuentaPorCobrar cpc = new CuentaPorCobrar(
            "CPC-001", clienteId, "FAC-001", "F001-001",
            Dinero.de("400.00", "PEN"), Dinero.cero("PEN"), vencimiento
        );

        assertThatThrownBy(() -> pago.aplicarACuentaPorCobrar(cpc, Dinero.de("500.00", "PEN")))
            .isInstanceOf(SaldoInsuficienteException.class)
            .hasMessageContaining("excede el saldo pendiente de la cuenta");

        assertThat(pago.montoAplicado().esCero()).isTrue();
        assertThat(pago.aplicaciones()).isEmpty();
        assertThat(cpc.aplicado().esCero()).isTrue();
    }

    @Test
    @DisplayName("Lanza MonedaIncompatibleException si se intenta aplicar con moneda distinta a la del pago o cuenta")
    void debeRechazarMonedasIncompatiblesEnAplicacion() {
        Pago pagoSoles = new Pago(
            "PAG-001", clienteId, Dinero.de("1000.00", "PEN"),
            MedioDePago.transferencia("TX-999"), hoy
        );

        CuentaPorCobrar cpcSoles = new CuentaPorCobrar(
            "CPC-001", clienteId, "FAC-001", "F001-001",
            Dinero.de("1000.00", "PEN"), Dinero.cero("PEN"), vencimiento
        );

        Dinero importeDolares = Dinero.de("100.00", "USD");

        assertThatThrownBy(() -> pagoSoles.aplicarACuentaPorCobrar(cpcSoles, importeDolares))
            .isInstanceOf(MonedaIncompatibleException.class);
    }

    @Test
    @DisplayName("Constructor de Pago rechaza fecha nula o datos invalidos con IllegalArgumentException")
    void debeRechazarDatosInvalidosEnConstructorDePago() {
        assertThatThrownBy(() -> new Pago("PAG-1", clienteId, Dinero.de("100.00", "PEN"), MedioDePago.efectivo(), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fecha del pago es obligatoria");

        assertThatThrownBy(() -> new Pago(null, clienteId, Dinero.de("100.00", "PEN"), MedioDePago.efectivo(), hoy))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("id del pago es obligatorio");

        assertThatThrownBy(() -> new Pago("PAG-1", null, Dinero.de("100.00", "PEN"), MedioDePago.efectivo(), hoy))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("clienteId es obligatorio");

        assertThatThrownBy(() -> new Pago("PAG-1", clienteId, Dinero.cero("PEN"), MedioDePago.efectivo(), hoy))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("monto del pago debe ser mayor a cero");
    }
}
