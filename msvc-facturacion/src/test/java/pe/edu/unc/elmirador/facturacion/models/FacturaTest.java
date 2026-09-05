package pe.edu.unc.elmirador.facturacion.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.facturacion.exceptions.DominioFacturacionException;
import pe.edu.unc.elmirador.facturacion.exceptions.FacturaInmutableException;
import pe.edu.unc.elmirador.facturacion.models.entity.Factura;
import pe.edu.unc.elmirador.facturacion.models.entity.LineaDeFactura;
import pe.edu.unc.elmirador.facturacion.models.entity.NotaDeCredito;
import pe.edu.unc.elmirador.facturacion.models.vo.ConceptoFacturable;
import pe.edu.unc.elmirador.facturacion.models.vo.Conformidad;
import pe.edu.unc.elmirador.facturacion.models.vo.Detraccion;
import pe.edu.unc.elmirador.facturacion.models.vo.Dinero;
import pe.edu.unc.elmirador.facturacion.models.vo.EstadoDeFactura;
import pe.edu.unc.elmirador.facturacion.models.vo.MotivoDeAjuste;
import pe.edu.unc.elmirador.facturacion.models.vo.NumeroDeComprobante;
import pe.edu.unc.elmirador.facturacion.models.vo.SnapshotComercial;

class FacturaTest {

    private final OffsetDateTime fechaBase = OffsetDateTime.of(2026, 9, 10, 16, 0, 0, 0, ZoneOffset.ofHours(-5));

    private SnapshotComercial snapshotPara(String ordenId, String clienteId, String monto) {
        return new SnapshotComercial(
            ordenId,
            clienteId,
            Dinero.de(monto, "PEN"),
            "PEN",
            fechaBase
        , "CREDITO", 30);
    }

    private Factura crearFacturaBloqueada(String facturaId, String ordenId, String clienteId) {
        SnapshotComercial snapshot = snapshotPara(ordenId, clienteId, "1821.60");
        Detraccion detraccion = new Detraccion(new BigDecimal("4"), Dinero.de("72.86", "PEN"), "00-123-456");
        return Factura.abrir(facturaId, ordenId, clienteId, snapshot, detraccion);
    }

    private Factura emitirFacturaEstandar(String facturaId, String ordenId, String clienteId, NumeroDeComprobante numero) {
        Factura factura = crearFacturaBloqueada(facturaId, ordenId, clienteId);
        factura.agregarLinea(new LineaDeFactura("L1", ordenId, ConceptoFacturable.FLETE, "Flete base", Dinero.de("1821.60", "PEN")));
        factura.registrarConformidad(Conformidad.conforme(fechaBase));
        factura.emitir(numero, fechaBase);
        return factura;
    }

    @Test
    @DisplayName("FAC-02: Una factura corresponde a exactamente una orden de servicio; ordenDeServicioId es obligatorio e inmutable")
    void fac02_ordenDeServicioEsObligatoriaEInmutable() {
        SnapshotComercial snapshot = snapshotPara("ORD-001", "CLI-001", "1000.00");
        Detraccion detraccion = Detraccion.sinDetraccion("PEN");

        assertThatThrownBy(() -> Factura.abrir("FAC-001", null, "CLI-001", snapshot, detraccion))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("El ordenDeServicioId es obligatorio");

        assertThatThrownBy(() -> Factura.abrir("FAC-001", "   ", "CLI-001", snapshot, detraccion))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("El ordenDeServicioId es obligatorio");

        Factura factura = Factura.abrir("FAC-001", "ORD-001", "CLI-001", snapshot, detraccion);
        assertThat(factura.correspondeA("ORD-001")).isTrue();
        assertThat(factura.correspondeA("ORD-002")).isFalse();
        assertThat(factura.ordenDeServicioId()).isEqualTo("ORD-001");
    }

    @Test
    @DisplayName("FAC-02: Una linea de factura correspondiente a otra orden de servicio se rechaza al agregarla")
    void fac02_rechazaLineaDeOtraOrdenDeServicio() {
        Factura factura = crearFacturaBloqueada("FAC-001", "ORD-001", "CLI-001");

        LineaDeFactura lineaDeOtraOrden = new LineaDeFactura(
            "L-OTRA", "ORD-002", ConceptoFacturable.ESTIBA, "Estiba", Dinero.de("100.00", "PEN")
        );

        assertThatThrownBy(() -> factura.agregarLinea(lineaDeOtraOrden))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("pertenece a la orden ORD-002 pero la factura es de la orden ORD-001");
    }

    @Test
    @DisplayName("FAC-03: Una factura emitida es inmutable; agregarLinea lanza FacturaInmutableException")
    void fac03_rechazaAgregarLineaAFacturaEmitida() {
        Factura factura = emitirFacturaEstandar("FAC-001", "ORD-001", "CLI-001", NumeroDeComprobante.de("F001", 1));

        LineaDeFactura nuevaLinea = new LineaDeFactura(
            "L2", "ORD-001", ConceptoFacturable.ESTIBA, "Nueva estiba", Dinero.de("100.00", "PEN")
        );

        assertThatThrownBy(() -> factura.agregarLinea(nuevaLinea))
            .isInstanceOf(FacturaInmutableException.class)
            .hasMessageContaining("No se pueden agregar lineas a una factura en estado EMITIDA");
    }

    @Test
    @DisplayName("FAC-03: Una factura emitida es inmutable; registrarConformidad lanza FacturaInmutableException")
    void fac03_rechazaRegistrarConformidadAFacturaEmitida() {
        Factura factura = emitirFacturaEstandar("FAC-001", "ORD-001", "CLI-001", NumeroDeComprobante.de("F001", 1));

        assertThatThrownBy(() -> factura.registrarConformidad(Conformidad.conforme(fechaBase)))
            .isInstanceOf(FacturaInmutableException.class)
            .hasMessageContaining("No se puede registrar conformidad en una factura en estado EMITIDA");
    }

    @Test
    @DisplayName("FAC-03: Una factura emitida es inmutable; re-emitir lanza FacturaInmutableException")
    void fac03_rechazaReemitirFacturaYaEmitida() {
        Factura factura = emitirFacturaEstandar("FAC-001", "ORD-001", "CLI-001", NumeroDeComprobante.de("F001", 1));

        assertThatThrownBy(() -> factura.emitir(NumeroDeComprobante.de("F001", 2), fechaBase))
            .isInstanceOf(FacturaInmutableException.class)
            .hasMessageContaining("La factura ya fue emitida y es inmutable");
    }

    @Test
    @DisplayName("FAC-03: La correccion de una factura emitida exige nota de credito")
    void fac03_permiteCorreccionMedianteNotaDeCredito() {
        Factura factura = emitirFacturaEstandar("FAC-001", "ORD-001", "CLI-001", NumeroDeComprobante.de("F001", 1));
        assertThat(factura.saldoAjustable()).isEqualTo(Dinero.de("1821.60", "PEN"));

        NotaDeCredito nc = NotaDeCredito.emitir(
            "NC-001", factura.id(), MotivoDeAjuste.ERROR_DE_FACTURACION,
            Dinero.de("200.00", "PEN"), factura.saldoAjustable(), fechaBase
        );

        factura.aplicarNotaDeCredito(nc);
        assertThat(factura.saldoAjustable()).isEqualTo(Dinero.de("1621.60", "PEN"));
        assertThat(factura.ajustesAplicados()).hasSize(1);
    }

    @Test
    @DisplayName("Borde: Tres ordenes generan tres facturas, cada una con su orden y sin compartir comprobante")
    void borde_tresOrdenesGeneranTresFacturasSinCompartirComprobante() {
        Factura f1 = emitirFacturaEstandar("FAC-1", "ORD-1", "CLI-1", NumeroDeComprobante.de("F001", 1));
        Factura f2 = emitirFacturaEstandar("FAC-2", "ORD-2", "CLI-2", NumeroDeComprobante.de("F001", 2));
        Factura f3 = emitirFacturaEstandar("FAC-3", "ORD-3", "CLI-3", NumeroDeComprobante.de("F001", 3));

        assertThat(f1.ordenDeServicioId()).isEqualTo("ORD-1");
        assertThat(f2.ordenDeServicioId()).isEqualTo("ORD-2");
        assertThat(f3.ordenDeServicioId()).isEqualTo("ORD-3");

        assertThat(f1.numeroDeComprobante()).isNotEqualTo(f2.numeroDeComprobante());
        assertThat(f2.numeroDeComprobante()).isNotEqualTo(f3.numeroDeComprobante());
    }

    @Test
    @DisplayName("Borde: El snapshot comercial no cambia tras emitir y no existe metodo de sustitucion")
    void borde_snapshotComercialPermaneceInmutableTrasEmision() {
        SnapshotComercial snapshot = snapshotPara("ORD-001", "CLI-001", "1821.60");
        Factura factura = emitirFacturaEstandar("FAC-001", "ORD-001", "CLI-001", NumeroDeComprobante.de("F001", 1));

        assertThat(factura.snapshotComercial()).isEqualTo(snapshot);
        assertThat(factura.snapshotComercial().tarifa()).isEqualTo(Dinero.de("1821.60", "PEN"));
    }

    @Test
    @DisplayName("Borde: Toda operacion con fecha nula lanza IllegalArgumentException (D1)")
    void borde_operacionConFechaNulaLanzaIllegalArgumentException() {
        Factura factura = emitirFacturaEstandar("FAC-001", "ORD-001", "CLI-001", NumeroDeComprobante.de("F001", 1));

        assertThatThrownBy(() -> factura.anular(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("La fecha de anulacion es obligatoria");
    }

    @Test
    @DisplayName("Factura: anular cambia estado de EMITIDA a ANULADA; anular factura BLOQUEADA lanza excepcion")
    void debeAnularFacturaEmitidaYRechazarAnularBloqueada() {
        Factura bloqueada = crearFacturaBloqueada("FAC-001", "ORD-001", "CLI-001");
        assertThatThrownBy(() -> bloqueada.anular(fechaBase))
            .isInstanceOf(DominioFacturacionException.class)
            .hasMessageContaining("Solo se puede anular una factura en estado EMITIDA");

        Factura emitida = emitirFacturaEstandar("FAC-002", "ORD-001", "CLI-001", NumeroDeComprobante.de("F001", 10));
        emitida.anular(fechaBase);
        assertThat(emitida.estado()).isEqualTo(EstadoDeFactura.ANULADA);

        LineaDeFactura linea = new LineaDeFactura("L2", "ORD-001", ConceptoFacturable.ESTIBA, "Estiba", Dinero.de("10.00", "PEN"));
        assertThatThrownBy(() -> emitida.agregarLinea(linea))
            .isInstanceOf(FacturaInmutableException.class);
    }
}
