package pe.edu.unc.elmirador.facturacion.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.facturacion.exceptions.MonedaIncompatibleException;
import pe.edu.unc.elmirador.facturacion.exceptions.MontoExcedeElSaldoException;
import pe.edu.unc.elmirador.facturacion.models.entity.Factura;
import pe.edu.unc.elmirador.facturacion.models.entity.LineaDeFactura;
import pe.edu.unc.elmirador.facturacion.models.entity.NotaDeCredito;
import pe.edu.unc.elmirador.facturacion.models.vo.ConceptoFacturable;
import pe.edu.unc.elmirador.facturacion.models.vo.Conformidad;
import pe.edu.unc.elmirador.facturacion.models.vo.Detraccion;
import pe.edu.unc.elmirador.facturacion.models.vo.Dinero;
import pe.edu.unc.elmirador.facturacion.models.vo.MotivoDeAjuste;
import pe.edu.unc.elmirador.facturacion.models.vo.NumeroDeComprobante;
import pe.edu.unc.elmirador.facturacion.models.vo.SnapshotComercial;

class NotaDeCreditoTest {

    private final OffsetDateTime fechaBase = OffsetDateTime.of(2026, 9, 10, 17, 0, 0, 0, ZoneOffset.ofHours(-5));

    private Factura crearFacturaEmitida(String facturaId, String monto) {
        SnapshotComercial snapshot = new SnapshotComercial(
            "ORD-001", "CLI-001", Dinero.de(monto, "PEN"), "PEN", fechaBase
        , "CREDITO", 30);
        Detraccion detraccion = Detraccion.sinDetraccion("PEN");
        Factura factura = Factura.abrir(facturaId, "ORD-001", "CLI-001", snapshot, detraccion);
        factura.agregarLinea(new LineaDeFactura("L1", "ORD-001", ConceptoFacturable.FLETE, "Flete", Dinero.de(monto, "PEN")));
        factura.registrarConformidad(Conformidad.conforme(fechaBase));
        factura.emitir(NumeroDeComprobante.de("F001", 1), fechaBase);
        return factura;
    }

    @Test
    @DisplayName("NCR-01: Nota de credito por encima del saldo ajustable de la factura lanza MontoExcedeElSaldoException")
    void ncr01_rechazaNotaDeCreditoMayorAlSaldoAjustable() {
        Dinero saldoAjustable = Dinero.de("1000.00", "PEN");
        Dinero montoExcedente = Dinero.de("1000.01", "PEN");

        assertThatThrownBy(() -> NotaDeCredito.emitir(
            "NC-001", "FAC-001", MotivoDeAjuste.DANIO, "Dano en mercaderia",
            montoExcedente, saldoAjustable, fechaBase
        ))
            .isInstanceOf(MontoExcedeElSaldoException.class)
            .hasMessageContaining("excede el saldo ajustable de la factura");
    }

    @Test
    @DisplayName("NCR-01: Nota de credito por el saldo exacto no lanza y se emite exitosamente")
    void ncr01_permiteNotaDeCreditoPorSaldoExacto() {
        Dinero saldoAjustable = Dinero.de("1000.00", "PEN");
        Dinero montoSumaTotal = Dinero.de("1000.00", "PEN");

        NotaDeCredito nc = NotaDeCredito.emitir(
            "NC-001", "FAC-001", MotivoDeAjuste.RECHAZO, "Rechazo total",
            montoSumaTotal, saldoAjustable, fechaBase
        );

        assertThat(nc.monto()).isEqualTo(saldoAjustable);
        assertThat(nc.motivo()).isEqualTo(MotivoDeAjuste.RECHAZO);
        assertThat(nc.facturaId()).isEqualTo("FAC-001");
    }

    @Test
    @DisplayName("NCR-01: emitir con saldoAjustable nulo lanza IllegalArgumentException (D2: no evadir con null)")
    void ncr01_rechazaSaldoAjustableNulo() {
        Dinero monto = Dinero.de("100.00", "PEN");

        assertThatThrownBy(() -> NotaDeCredito.emitir(
            "NC-001", "FAC-001", MotivoDeAjuste.FALTANTE, "Faltante",
            monto, null, fechaBase
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("El saldo ajustable de la factura es obligatorio");
    }

    @Test
    @DisplayName("Borde: Dos notas de credito sucesivas; la segunda se compara contra el saldo ya reducido por la primera")
    void borde_dosNotasDeCreditoSucesivasReducenSaldo() {
        Factura factura = crearFacturaEmitida("FAC-001", "1000.00");
        assertThat(factura.saldoAjustable()).isEqualTo(Dinero.de("1000.00", "PEN"));

        // 1ra Nota de credito por 600.00
        NotaDeCredito nc1 = NotaDeCredito.emitir(
            "NC-001", factura.id(), MotivoDeAjuste.DANIO, "Primer danio",
            Dinero.de("600.00", "PEN"), factura.saldoAjustable(), fechaBase
        );
        factura.aplicarNotaDeCredito(nc1);
        assertThat(factura.saldoAjustable()).isEqualTo(Dinero.de("400.00", "PEN"));

        // Intento de 2da Nota de credito por 400.01 (excede el saldo restante de 400.00)
        assertThatThrownBy(() -> NotaDeCredito.emitir(
            "NC-002", factura.id(), MotivoDeAjuste.FALTANTE, "Segundo faltante",
            Dinero.de("400.01", "PEN"), factura.saldoAjustable(), fechaBase
        ))
            .isInstanceOf(MontoExcedeElSaldoException.class)
            .hasMessageContaining("excede el saldo ajustable de la factura");

        // 2da Nota de credito por 400.00 exactos (procede exitosamente)
        NotaDeCredito nc2 = NotaDeCredito.emitir(
            "NC-002", factura.id(), MotivoDeAjuste.FALTANTE, "Segundo faltante",
            Dinero.de("400.00", "PEN"), factura.saldoAjustable(), fechaBase
        );
        factura.aplicarNotaDeCredito(nc2);
        assertThat(factura.saldoAjustable()).isEqualTo(Dinero.de("0.00", "PEN"));
    }

    @Test
    @DisplayName("Borde: Nota de credito con fecha nula lanza IllegalArgumentException (D1)")
    void borde_notaDeCreditoConFechaNulaLanzaIllegalArgumentException() {
        Dinero saldo = Dinero.de("500.00", "PEN");
        Dinero monto = Dinero.de("100.00", "PEN");

        assertThatThrownBy(() -> NotaDeCredito.emitir(
            "NC-001", "FAC-001", MotivoDeAjuste.ERROR_DE_FACTURACION,
            monto, saldo, null
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("La fecha de emision es obligatoria");
    }

    @Test
    @DisplayName("Borde: Nota de credito con moneda incompatible lanza MonedaIncompatibleException")
    void borde_notaDeCreditoConMonedaIncompatibleLanzaExcepcion() {
        Dinero saldoPen = Dinero.de("500.00", "PEN");
        Dinero montoUsd = Dinero.de("100.00", "USD");

        assertThatThrownBy(() -> NotaDeCredito.emitir(
            "NC-001", "FAC-001", MotivoDeAjuste.ERROR_DE_FACTURACION,
            montoUsd, saldoPen, fechaBase
        ))
            .isInstanceOf(MonedaIncompatibleException.class);
    }
}
