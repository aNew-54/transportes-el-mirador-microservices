package pe.edu.unc.elmirador.ejecucion.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.ejecucion.exceptions.GastoSinComprobanteException;
import pe.edu.unc.elmirador.ejecucion.exceptions.LiquidacionAprobadaException;
import pe.edu.unc.elmirador.ejecucion.models.entity.GastoDeRuta;
import pe.edu.unc.elmirador.ejecucion.models.entity.LiquidacionDeViaje;
import pe.edu.unc.elmirador.ejecucion.models.vo.Comprobante;
import pe.edu.unc.elmirador.ejecucion.models.vo.ConceptoDeGasto;
import pe.edu.unc.elmirador.ejecucion.models.vo.Dinero;
import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoDeLiquidacion;
import pe.edu.unc.elmirador.ejecucion.models.vo.Saldo;
import pe.edu.unc.elmirador.ejecucion.models.vo.SignoDeSaldo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LiquidacionDeViajeTest {

    private static final ZoneOffset LIMA = ZoneOffset.ofHours(-5);
    private static final OffsetDateTime T08_00 = OffsetDateTime.of(2026, 9, 10, 8, 0, 0, 0, LIMA);
    private static final OffsetDateTime T14_00 = OffsetDateTime.of(2026, 9, 10, 14, 0, 0, 0, LIMA);

    private Comprobante crearComprobanteValido() {
        return new Comprobante("FACTURA", "F001-000123", T08_00);
    }

    // ==========================================
    // LIQ-01: Gasto exige comprobante
    // ==========================================

    @Test
    @DisplayName("[LIQ-01] GastoDeRuta sin comprobante no se puede construir y lanza GastoSinComprobanteException")
    void gastoSinComprobanteLanzaExcepcion_LIQ01() {
        assertThatThrownBy(() -> new GastoDeRuta(
                "GST-01",
                ConceptoDeGasto.COMBUSTIBLE,
                Dinero.de("120.00", "PEN"),
                null,
                "Diesel Grifo Repsol"
        ))
                .as("[LIQ-01] GastoDeRuta sin comprobante debe fallar")
                .isInstanceOf(GastoSinComprobanteException.class)
                .hasMessageContaining("debe contar con comprobante");
    }

    @Test
    @DisplayName("[LIQ-01] Comprobante con campos incompletos lanza IllegalArgumentException")
    void comprobanteIncompletoLanzaExcepcion_LIQ01() {
        assertThatThrownBy(() -> new Comprobante("", "F001-1", T08_00))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tipo");

        assertThatThrownBy(() -> new Comprobante("FACTURA", "   ", T08_00))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("numero");

        assertThatThrownBy(() -> new Comprobante("FACTURA", "F001-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fecha");
    }

    // ==========================================
    // LIQ-02: Saldo se calcula y nunca se almacena (D8)
    // ==========================================

    @Test
    @DisplayName("[LIQ-02] [D8] LiquidacionDeViaje NO declara ningun campo llamado saldo ni setter por reflexion")
    void noDeclaraCampoSaldoNiSetterPorReflexion_LIQ02() {
        Field[] campos = LiquidacionDeViaje.class.getDeclaredFields();
        boolean tieneCampoSaldo = Arrays.stream(campos)
                .anyMatch(c -> c.getName().equalsIgnoreCase("saldo"));

        assertThat(tieneCampoSaldo)
                .as("[LIQ-02] [D8] LiquidacionDeViaje NO debe declarar ningun campo llamado saldo")
                .isFalse();

        Method[] metodos = LiquidacionDeViaje.class.getDeclaredMethods();
        boolean tieneSetterSaldo = Arrays.stream(metodos)
                .anyMatch(m -> m.getName().equalsIgnoreCase("setSaldo"));

        assertThat(tieneSetterSaldo)
                .as("[LIQ-02] LiquidacionDeViaje NO debe declarar setters para saldo")
                .isFalse();
    }

    @Test
    @DisplayName("[LIQ-02] saldo se recalcula dinamicamente tras rendir cada nuevo gasto")
    void saldoSeRecalculaTrasRendirGastos_LIQ02() {
        LiquidacionDeViaje liquidacion = LiquidacionDeViaje.abrir("VIA-100", "CON-01", Dinero.de("500.00", "PEN"));

        // Inicial: anticipo 500, gastos 0 => a favor de la empresa 500
        Saldo saldo0 = liquidacion.saldo();
        assertThat(saldo0.signo()).isEqualTo(SignoDeSaldo.A_FAVOR_DE_LA_EMPRESA);
        assertThat(saldo0.importe()).isEqualTo(Dinero.de("500.00", "PEN"));

        // Gasto 1: 200 => sobra anticipo 300
        GastoDeRuta gasto1 = new GastoDeRuta("G1", ConceptoDeGasto.COMBUSTIBLE, Dinero.de("200.00", "PEN"), crearComprobanteValido(), "Combustible");
        liquidacion.rendirGasto(gasto1);

        Saldo saldo1 = liquidacion.saldo();
        assertThat(saldo1.signo()).isEqualTo(SignoDeSaldo.A_FAVOR_DE_LA_EMPRESA);
        assertThat(saldo1.importe()).isEqualTo(Dinero.de("300.00", "PEN"));

        // Gasto 2: 300 => total 500, saldo exacto SALDADO
        GastoDeRuta gasto2 = new GastoDeRuta("G2", ConceptoDeGasto.PEAJE, Dinero.de("300.00", "PEN"), crearComprobanteValido(), "Peajes");
        liquidacion.rendirGasto(gasto2);

        Saldo saldo2 = liquidacion.saldo();
        assertThat(saldo2.signo()).isEqualTo(SignoDeSaldo.SALDADO);
        assertThat(saldo2.importe().esCero()).isTrue();

        // Gasto 3: 150 => total 650, a favor del conductor 150
        GastoDeRuta gasto3 = new GastoDeRuta("G3", ConceptoDeGasto.COCHERA, Dinero.de("150.00", "PEN"), crearComprobanteValido(), "Cochera");
        liquidacion.rendirGasto(gasto3);

        Saldo saldo3 = liquidacion.saldo();
        assertThat(saldo3.signo()).isEqualTo(SignoDeSaldo.A_FAVOR_DEL_CONDUCTOR);
        assertThat(saldo3.importe()).isEqualTo(Dinero.de("150.00", "PEN"));
    }

    // ==========================================
    // LIQ-03: Inmutabilidad de liquidacion aprobada
    // ==========================================

    @Test
    @DisplayName("[LIQ-03] Sobre liquidacion APROBADA rendirGasto lanza LiquidacionAprobadaException")
    void rendirGastoSobreLiquidacionAprobadaLanzaExcepcion_LIQ03() {
        LiquidacionDeViaje liquidacion = LiquidacionDeViaje.abrir("VIA-100", "CON-01", Dinero.de("200.00", "PEN"));
        liquidacion.aprobar(T14_00);

        GastoDeRuta gasto = new GastoDeRuta("G1", ConceptoDeGasto.PEAJE, Dinero.de("50.00", "PEN"), crearComprobanteValido(), "Peaje");

        assertThatThrownBy(() -> liquidacion.rendirGasto(gasto))
                .as("[LIQ-03] rendirGasto sobre liquidacion aprobada debe fallar")
                .isInstanceOf(LiquidacionAprobadaException.class)
                .hasMessageContaining("liquidacion aprobada");
    }

    @Test
    @DisplayName("[LIQ-03] Sobre liquidacion APROBADA volver a aprobar lanza LiquidacionAprobadaException")
    void aprobarSobreLiquidacionAprobadaLanzaExcepcion_LIQ03() {
        LiquidacionDeViaje liquidacion = LiquidacionDeViaje.abrir("VIA-100", "CON-01", Dinero.de("200.00", "PEN"));
        liquidacion.aprobar(T14_00);

        assertThatThrownBy(() -> liquidacion.aprobar(T14_00.plusHours(1)))
                .as("[LIQ-03] aprobar sobre liquidacion ya aprobada debe fallar")
                .isInstanceOf(LiquidacionAprobadaException.class)
                .hasMessageContaining("ya esta aprobada");
    }

    @Test
    @DisplayName("[LIQ-03] Sobre liquidacion APROBADA observar lanza LiquidacionAprobadaException")
    void observarSobreLiquidacionAprobadaLanzaExcepcion_LIQ03() {
        LiquidacionDeViaje liquidacion = LiquidacionDeViaje.abrir("VIA-100", "CON-01", Dinero.de("200.00", "PEN"));
        liquidacion.aprobar(T14_00);

        assertThatThrownBy(() -> liquidacion.observar("Comprobante ilegible"))
                .as("[LIQ-03] observar sobre liquidacion aprobada debe fallar")
                .isInstanceOf(LiquidacionAprobadaException.class)
                .hasMessageContaining("No se puede observar una liquidacion aprobada");
    }

    @Test
    @DisplayName("Observar liquidacion abierta cambia estado a OBSERVADA")
    void observarLiquidacionAbiertaPasaAObservada() {
        LiquidacionDeViaje liquidacion = LiquidacionDeViaje.abrir("VIA-100", "CON-01", Dinero.de("200.00", "PEN"));
        liquidacion.observar("Falta factura de combustible");

        assertThat(liquidacion.getEstado()).isEqualTo(EstadoDeLiquidacion.OBSERVADA);
        assertThat(liquidacion.getMotivoObservacion()).isEqualTo("Falta factura de combustible");
        assertThat(liquidacion.estaPendiente()).isTrue();
    }

    // ==========================================
    // Borde obligatorio: Viaje con relevo (dos liquidaciones)
    // ==========================================

    @Test
    @DisplayName("Viaje con relevo: dos liquidaciones independientes, aprobar una no aprueba la otra")
    void viajeConRelevoDosLiquidacionesIndependientes() {
        String viajeId = "VIA-2026-RELEVO-01";
        LiquidacionDeViaje liqConductor1 = LiquidacionDeViaje.abrir(viajeId, "CON-001", Dinero.de("400.00", "PEN"));
        LiquidacionDeViaje liqConductor2 = LiquidacionDeViaje.abrir(viajeId, "CON-002", Dinero.de("350.00", "PEN"));

        // Se aprueba solo la primera
        liqConductor1.aprobar(T14_00);

        assertThat(liqConductor1.getEstado()).isEqualTo(EstadoDeLiquidacion.APROBADA);
        assertThat(liqConductor1.estaPendiente()).isFalse();

        assertThat(liqConductor2.getEstado()).isEqualTo(EstadoDeLiquidacion.ABIERTA);
        assertThat(liqConductor2.estaPendiente()).isTrue();
    }

    @Test
    @DisplayName("[D1] Aprobar con fecha nula lanza IllegalArgumentException")
    void aprobarConFechaNulaLanzaExcepcion() {
        LiquidacionDeViaje liquidacion = LiquidacionDeViaje.abrir("VIA-100", "CON-01", Dinero.de("200.00", "PEN"));

        assertThatThrownBy(() -> liquidacion.aprobar(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aprobacion");
    }
}
