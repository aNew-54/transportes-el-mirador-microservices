package pe.edu.unc.elmirador.cobranza.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.cobranza.exceptions.DominioCobranzaException;
import pe.edu.unc.elmirador.cobranza.exceptions.ImportesInconsistentesException;
import pe.edu.unc.elmirador.cobranza.exceptions.MonedaIncompatibleException;
import pe.edu.unc.elmirador.cobranza.exceptions.SaldoInsuficienteException;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaPorCobrar;
import pe.edu.unc.elmirador.cobranza.models.vo.Dinero;
import pe.edu.unc.elmirador.cobranza.models.vo.EstadoDeDocumento;

class CuentaPorCobrarTest {

    private final LocalDate vencimiento = LocalDate.of(2026, 10, 10);

    @Test
    @DisplayName("FAC-04 en frontera: Constructor lanza ImportesInconsistentesException si montoNeto + detraccion != total")
    void debeRechazarImportesInconsistentesEnConstructor() {
        Dinero total = Dinero.de("1821.60", "PEN");
        Dinero detraccion = Dinero.de("72.86", "PEN");
        Dinero montoNetoErroneo = Dinero.de("1700.00", "PEN"); // 1700.00 + 72.86 != 1821.60

        assertThatThrownBy(() -> new CuentaPorCobrar(
            "CPC-001", "CLI-0007", "FAC-001", "F001-001",
            total, detraccion, montoNetoErroneo, vencimiento
        ))
        .isInstanceOf(ImportesInconsistentesException.class)
        .hasMessageContaining("no iguala el total");
    }

    @Test
    @DisplayName("Constructor lanza MonedaIncompatibleException si las monedas de los importes difieren")
    void debeRechazarMonedasIncompatiblesEnConstructor() {
        Dinero total = Dinero.de("1000.00", "PEN");
        Dinero detraccion = Dinero.de("40.00", "USD");
        Dinero montoNeto = Dinero.de("960.00", "PEN");

        assertThatThrownBy(() -> new CuentaPorCobrar(
            "CPC-001", "CLI-0007", "FAC-001", "F001-001",
            total, detraccion, montoNeto, vencimiento
        ))
        .isInstanceOf(MonedaIncompatibleException.class);
    }

    @Test
    @DisplayName("Constructor calcula correctamente montoNeto y saldo inicial")
    void debeCalcularMontoNetoYSaldoInicial() {
        Dinero total = Dinero.de("1821.60", "PEN");
        Dinero detraccion = Dinero.de("72.86", "PEN");
        Dinero montoNeto = Dinero.de("1748.74", "PEN");

        CuentaPorCobrar cpc = new CuentaPorCobrar(
            "CPC-001", "CLI-0007", "FAC-001", "F001-001",
            total, detraccion, montoNeto, vencimiento
        );

        assertThat(cpc.montoNeto()).isEqualTo(montoNeto);
        assertThat(cpc.saldo()).isEqualTo(montoNeto);
        assertThat(cpc.aplicado().esCero()).isTrue();
        assertThat(cpc.detraccionDepositada()).isFalse();
    }

    @Test
    @DisplayName("CCC-02: Aplicar importe valido suma al aplicado y reduce el saldo")
    void ccc02_debeAplicarImporteValido() {
        CuentaPorCobrar cpc = new CuentaPorCobrar(
            "CPC-001", "CLI-0007", "FAC-001", "F001-001",
            Dinero.de("1000.00", "PEN"), Dinero.de("100.00", "PEN"), Dinero.de("900.00", "PEN"), vencimiento
        );

        cpc.aplicar(Dinero.de("400.00", "PEN"));

        assertThat(cpc.aplicado()).isEqualTo(Dinero.de("400.00", "PEN"));
        assertThat(cpc.saldo()).isEqualTo(Dinero.de("500.00", "PEN"));
    }

    @Test
    @DisplayName("CCC-02: Dejar el saldo exactamente en cero no lanza y cancela el saldo pendiente")
    void ccc02_debePermitirAplicarImporteExactoAlSaldo() {
        CuentaPorCobrar cpc = new CuentaPorCobrar(
            "CPC-001", "CLI-0007", "FAC-001", "F001-001",
            Dinero.de("1000.00", "PEN"), Dinero.de("100.00", "PEN"), Dinero.de("900.00", "PEN"), vencimiento
        );

        cpc.aplicar(Dinero.de("900.00", "PEN"));

        assertThat(cpc.saldo().esCero()).isTrue();
        assertThat(cpc.aplicado()).isEqualTo(Dinero.de("900.00", "PEN"));
    }

    @Test
    @DisplayName("CCC-02: Aplicar importe que dejaria saldo negativo lanza SaldoInsuficienteException y NO altera el aplicado")
    void ccc02_debeFallarYSinAlterarAplicadoCuandoExcedeSaldo() {
        CuentaPorCobrar cpc = new CuentaPorCobrar(
            "CPC-001", "CLI-0007", "FAC-001", "F001-001",
            Dinero.de("1000.00", "PEN"), Dinero.de("100.00", "PEN"), Dinero.de("900.00", "PEN"), vencimiento
        );

        cpc.aplicar(Dinero.de("500.00", "PEN"));
        Dinero aplicadoAntes = cpc.aplicado();
        Dinero saldoAntes = cpc.saldo();

        assertThatThrownBy(() -> cpc.aplicar(Dinero.de("400.01", "PEN")))
            .isInstanceOf(SaldoInsuficienteException.class)
            .hasMessageContaining("excede el saldo pendiente");

        assertThat(cpc.aplicado()).isEqualTo(aplicadoAntes);
        assertThat(cpc.saldo()).isEqualTo(saldoAntes);
    }

    @Test
    @DisplayName("CCC-03: Caso 1 - sin pago completo y sin deposito de detraccion -> estaCancelada es false")
    void ccc03_caso1_sinPagoCompletoYSinDetraccion() {
        CuentaPorCobrar cpc = new CuentaPorCobrar(
            "CPC-001", "CLI-0007", "FAC-001", "F001-001",
            Dinero.de("1000.00", "PEN"), Dinero.de("100.00", "PEN"), Dinero.de("900.00", "PEN"), vencimiento
        );

        assertThat(cpc.estaCancelada()).isFalse();
    }

    @Test
    @DisplayName("CCC-03: Caso 2 - con pago completo y sin deposito de detraccion -> estaCancelada es false")
    void ccc03_caso2_conPagoCompletoSinDetraccion() {
        CuentaPorCobrar cpc = new CuentaPorCobrar(
            "CPC-001", "CLI-0007", "FAC-001", "F001-001",
            Dinero.de("1000.00", "PEN"), Dinero.de("100.00", "PEN"), Dinero.de("900.00", "PEN"), vencimiento
        );

        cpc.aplicar(Dinero.de("900.00", "PEN"));

        assertThat(cpc.saldo().esCero()).isTrue();
        assertThat(cpc.detraccionDepositada()).isFalse();
        assertThat(cpc.estaCancelada()).isFalse();
    }

    @Test
    @DisplayName("CCC-03: Caso 3 - con deposito de detraccion pero con saldo pendiente -> estaCancelada es false")
    void ccc03_caso3_conDetraccionDepositadaYSaldosPendientes() {
        CuentaPorCobrar cpc = new CuentaPorCobrar(
            "CPC-001", "CLI-0007", "FAC-001", "F001-001",
            Dinero.de("1000.00", "PEN"), Dinero.de("100.00", "PEN"), Dinero.de("900.00", "PEN"), vencimiento
        );

        cpc.registrarDepositoDeDetraccion();

        assertThat(cpc.detraccionDepositada()).isTrue();
        assertThat(cpc.saldo().esCero()).isFalse();
        assertThat(cpc.estaCancelada()).isFalse();
    }

    @Test
    @DisplayName("CCC-03: Caso 4 - con pago completo y deposito de detraccion -> estaCancelada es true")
    void ccc03_caso4_conPagoCompletoYDetraccionDepositada() {
        CuentaPorCobrar cpc = new CuentaPorCobrar(
            "CPC-001", "CLI-0007", "FAC-001", "F001-001",
            Dinero.de("1000.00", "PEN"), Dinero.de("100.00", "PEN"), Dinero.de("900.00", "PEN"), vencimiento
        );

        cpc.aplicar(Dinero.de("900.00", "PEN"));
        cpc.registrarDepositoDeDetraccion();

        assertThat(cpc.estaCancelada()).isTrue();
    }

    @Test
    @DisplayName("CCC-03: Caso 5 - cuenta sin detraccion se cancela unicamente con el pago completo")
    void ccc03_caso5_cuentaSinDetraccionSeCancelaSoloConPago() {
        CuentaPorCobrar cpc = new CuentaPorCobrar(
            "CPC-002", "CLI-0007", "FAC-002", "F001-002",
            Dinero.de("500.00", "PEN"), Dinero.cero("PEN"), Dinero.de("500.00", "PEN"), vencimiento
        );

        assertThat(cpc.estaCancelada()).isFalse();

        cpc.aplicar(Dinero.de("500.00", "PEN"));

        assertThat(cpc.estaCancelada()).isTrue();
    }

    @Test
    @DisplayName("Registrar deposito de detraccion en cuenta con detraccion cero lanza DominioCobranzaException")
    void debeFallarAlRegistrarDetraccionEnCuentaSinDetraccion() {
        CuentaPorCobrar cpc = new CuentaPorCobrar(
            "CPC-002", "CLI-0007", "FAC-002", "F001-002",
            Dinero.de("500.00", "PEN"), Dinero.cero("PEN"), Dinero.de("500.00", "PEN"), vencimiento
        );

        assertThatThrownBy(cpc::registrarDepositoDeDetraccion)
            .isInstanceOf(DominioCobranzaException.class)
            .hasMessageContaining("sin detraccion");
    }

    @Test
    @DisplayName("Factura cobrada en dos partes regulariza el saldo y se cancela al depositar detraccion")
    void debePermitirCobroDeFacturaEnDosPagos() {
        CuentaPorCobrar cpc = new CuentaPorCobrar(
            "CPC-001", "CLI-0007", "FAC-001", "F001-001",
            Dinero.de("1000.00", "PEN"), Dinero.de("100.00", "PEN"), Dinero.de("900.00", "PEN"), vencimiento
        );

        cpc.aplicar(Dinero.de("400.00", "PEN"));
        assertThat(cpc.saldo()).isEqualTo(Dinero.de("500.00", "PEN"));
        assertThat(cpc.estaCancelada()).isFalse();

        cpc.aplicar(Dinero.de("500.00", "PEN"));
        assertThat(cpc.saldo().esCero()).isTrue();
        assertThat(cpc.estaCancelada()).isFalse();

        cpc.registrarDepositoDeDetraccion();
        assertThat(cpc.estaCancelada()).isTrue();
    }

    @Test
    @DisplayName("Evalua estadoEn segun la fecha y el estado de cancelacion")
    void debeEvaluarEstadoEn() {
        CuentaPorCobrar cpc = new CuentaPorCobrar(
            "CPC-001", "CLI-0007", "FAC-001", "F001-001",
            Dinero.de("1000.00", "PEN"), Dinero.cero("PEN"), Dinero.de("1000.00", "PEN"), vencimiento
        );

        LocalDate antes = LocalDate.of(2026, 10, 5);
        LocalDate elDia = LocalDate.of(2026, 10, 10);
        LocalDate despues = LocalDate.of(2026, 10, 11);

        assertThat(cpc.estadoEn(antes)).isEqualTo(EstadoDeDocumento.VIGENTE);
        assertThat(cpc.estadoEn(elDia)).isEqualTo(EstadoDeDocumento.VIGENTE);
        assertThat(cpc.estadoEn(despues)).isEqualTo(EstadoDeDocumento.VENCIDA);

        cpc.aplicar(Dinero.de("1000.00", "PEN"));
        assertThat(cpc.estadoEn(despues)).isEqualTo(EstadoDeDocumento.CANCELADO);
    }

    @Test
    @DisplayName("Operaciones dependientes del tiempo lanzan IllegalArgumentException si la fecha es nula")
    void debeRechazarFechasNulas() {
        CuentaPorCobrar cpc = new CuentaPorCobrar(
            "CPC-001", "CLI-0007", "FAC-001", "F001-001",
            Dinero.de("1000.00", "PEN"), Dinero.cero("PEN"), Dinero.de("1000.00", "PEN"), vencimiento
        );

        assertThatThrownBy(() -> cpc.estadoEn(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fecha es obligatoria");

        assertThatThrownBy(() -> cpc.diasDeAtraso(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fecha es obligatoria");
    }
}
