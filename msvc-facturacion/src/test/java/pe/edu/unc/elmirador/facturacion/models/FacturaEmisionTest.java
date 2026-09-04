package pe.edu.unc.elmirador.facturacion.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.facturacion.exceptions.DominioFacturacionException;
import pe.edu.unc.elmirador.facturacion.exceptions.EmisionSinConformidadException;
import pe.edu.unc.elmirador.facturacion.exceptions.ImportesInconsistentesException;
import pe.edu.unc.elmirador.facturacion.exceptions.IncidenciaSinResolverException;
import pe.edu.unc.elmirador.facturacion.models.entity.Factura;
import pe.edu.unc.elmirador.facturacion.models.entity.LineaDeFactura;
import pe.edu.unc.elmirador.facturacion.models.vo.ConceptoFacturable;
import pe.edu.unc.elmirador.facturacion.models.vo.Conformidad;
import pe.edu.unc.elmirador.facturacion.models.vo.Detraccion;
import pe.edu.unc.elmirador.facturacion.models.vo.Dinero;
import pe.edu.unc.elmirador.facturacion.models.vo.EstadoDeFactura;
import pe.edu.unc.elmirador.facturacion.models.vo.NumeroDeComprobante;
import pe.edu.unc.elmirador.facturacion.models.vo.SnapshotComercial;

class FacturaEmisionTest {

    private final OffsetDateTime fechaBase = OffsetDateTime.of(2026, 9, 10, 16, 30, 0, 0, ZoneOffset.ofHours(-5));

    private Factura crearFactura(Detraccion detraccion, Dinero total) {
        SnapshotComercial snapshot = new SnapshotComercial(
            "ORD-001",
            "CLI-001",
            total,
            total.codigoMoneda(),
            fechaBase
        );
        Factura factura = Factura.abrir("FAC-001", "ORD-001", "CLI-001", snapshot, detraccion);
        factura.agregarLinea(new LineaDeFactura("L1", "ORD-001", ConceptoFacturable.FLETE, "Flete base", total));
        return factura;
    }

    @Test
    @DisplayName("FAC-01: Emitir sin conformidad de entrega registrada lanza EmisionSinConformidadException")
    void fac01_rechazaEmisionSinConformidadRegistrada() {
        Detraccion detraccion = new Detraccion(new BigDecimal("4"), Dinero.de("72.86", "PEN"), "00-123-456");
        Factura factura = crearFactura(detraccion, Dinero.de("1821.60", "PEN"));

        assertThat(factura.conformidad().registrada()).isFalse();

        assertThatThrownBy(() -> factura.emitir(NumeroDeComprobante.de("F001", 1), fechaBase))
            .isInstanceOf(EmisionSinConformidadException.class)
            .hasMessageContaining("sin conformidad de entrega registrada");

        assertThat(factura.estado()).isEqualTo(EstadoDeFactura.BLOQUEADA);
    }

    @Test
    @DisplayName("FAC-01: Con conformidad registrada y sin incidencias pendientes, la emision procede exitosamente")
    void fac01_permiteEmisionConConformidadRegistrada() {
        Detraccion detraccion = new Detraccion(new BigDecimal("4"), Dinero.de("72.86", "PEN"), "00-123-456");
        Factura factura = crearFactura(detraccion, Dinero.de("1821.60", "PEN"));

        factura.registrarConformidad(Conformidad.conforme(fechaBase));
        NumeroDeComprobante comprobante = NumeroDeComprobante.de("F001", 310);

        factura.emitir(comprobante, fechaBase);

        assertThat(factura.estado()).isEqualTo(EstadoDeFactura.EMITIDA);
        assertThat(factura.numeroDeComprobante()).isEqualTo(comprobante);
        assertThat(factura.fechaDeEmision()).isEqualTo(fechaBase);
    }

    @Test
    @DisplayName("FAC-01: emitirFalsoFlete sin conformidad si emite por ser cancelacion posterior al despacho")
    void fac01_emitirFalsoFleteSinConformidadSiEmite() {
        Detraccion detraccion = Detraccion.sinDetraccion("PEN");
        SnapshotComercial snapshot = new SnapshotComercial(
            "ORD-001", "CLI-001", Dinero.de("500.00", "PEN"), "PEN", fechaBase
        );
        Factura factura = Factura.abrir("FAC-FF-001", "ORD-001", "CLI-001", snapshot, detraccion);
        factura.agregarLinea(new LineaDeFactura("L-FF", "ORD-001", ConceptoFacturable.FALSO_FLETE, "Falso flete", Dinero.de("500.00", "PEN")));

        assertThat(factura.conformidad().registrada()).isFalse();

        NumeroDeComprobante comprobante = NumeroDeComprobante.de("F001", 500);
        factura.emitirFalsoFlete(comprobante, fechaBase);

        assertThat(factura.estado()).isEqualTo(EstadoDeFactura.EMITIDA);
        assertThat(factura.esFalsoFlete()).isTrue();
        assertThat(factura.numeroDeComprobante()).isEqualTo(comprobante);
    }

    @Test
    @DisplayName("FAC-01: emitirFalsoFlete exige exactamente una linea de concepto FALSO_FLETE")
    void fac01_emitirFalsoFleteExigeLineaExclusivaDeFalsoFlete() {
        Detraccion detraccion = Detraccion.sinDetraccion("PEN");
        SnapshotComercial snapshot = new SnapshotComercial(
            "ORD-001", "CLI-001", Dinero.de("500.00", "PEN"), "PEN", fechaBase
        );
        Factura factura = Factura.abrir("FAC-FF-002", "ORD-001", "CLI-001", snapshot, detraccion);
        factura.agregarLinea(new LineaDeFactura("L1", "ORD-001", ConceptoFacturable.FLETE, "Flete regular", Dinero.de("500.00", "PEN")));

        NumeroDeComprobante comprobante = NumeroDeComprobante.de("F001", 501);
        assertThatThrownBy(() -> factura.emitirFalsoFlete(comprobante, fechaBase))
            .isInstanceOf(DominioFacturacionException.class)
            .hasMessageContaining("exige exactamente una linea con concepto FALSO_FLETE");
    }

    @Test
    @DisplayName("FAC-04: Con detraccion.monto que no cuadra con total y porcentaje, emitir lanza ImportesInconsistentesException")
    void fac04_rechazaEmisionSiDetraccionNoCuadra() {
        // Para total 1821.60 y 4%, la detraccion real es 72.86. Pasamos 70.00 que no cuadra.
        Detraccion detraccionInconsistente = new Detraccion(new BigDecimal("4"), Dinero.de("70.00", "PEN"), "00-123-456");
        Factura factura = crearFactura(detraccionInconsistente, Dinero.de("1821.60", "PEN"));
        factura.registrarConformidad(Conformidad.conforme(fechaBase));

        assertThatThrownBy(() -> factura.emitir(NumeroDeComprobante.de("F001", 1), fechaBase))
            .isInstanceOf(ImportesInconsistentesException.class)
            .hasMessageContaining("no coincide con el porcentaje (4%) del total (1821.60)");

        assertThat(factura.estado()).isEqualTo(EstadoDeFactura.BLOQUEADA);
    }

    @Test
    @DisplayName("FAC-04: Con importes exactos (total 1821.60, detraccion 4% de 72.86, neto 1748.74) emite exitosamente")
    void fac04_permiteEmisionConImportesExactos() {
        Detraccion detraccion = new Detraccion(new BigDecimal("4"), Dinero.de("72.86", "PEN"), "00-123-456");
        Factura factura = crearFactura(detraccion, Dinero.de("1821.60", "PEN"));
        factura.registrarConformidad(Conformidad.conforme(fechaBase));

        assertThat(factura.total()).isEqualTo(Dinero.de("1821.60", "PEN"));
        assertThat(factura.montoNeto()).isEqualTo(Dinero.de("1748.74", "PEN"));
        assertThat(factura.detraccion().monto()).isEqualTo(Dinero.de("72.86", "PEN"));

        factura.emitir(NumeroDeComprobante.de("F001", 1), fechaBase);
        assertThat(factura.estado()).isEqualTo(EstadoDeFactura.EMITIDA);
    }

    @Test
    @DisplayName("FAC-05: Con incidenciasSinResolver no vacia, emitir lanza IncidenciaSinResolverException y la factura SIGUE BLOQUEADA")
    void fac05_rechazaEmisionConIncidenciasSinResolverYMantieneEstadoBloqueada() {
        Detraccion detraccion = Detraccion.sinDetraccion("PEN");
        Factura factura = crearFactura(detraccion, Dinero.de("1000.00", "PEN"));

        List<String> incidencias = List.of("DANO_PARCIAL_EN_PALLET_2");
        factura.registrarConformidad(Conformidad.conIncidencias(incidencias, fechaBase));

        assertThat(factura.estado()).isEqualTo(EstadoDeFactura.BLOQUEADA);

        assertThatThrownBy(() -> factura.emitir(NumeroDeComprobante.de("F001", 1), fechaBase))
            .isInstanceOf(IncidenciaSinResolverException.class)
            .hasMessageContaining("incidencias sin resolver");

        // D6: Se valida todo antes de mutar nada -> sigue BLOQUEADA
        assertThat(factura.estado()).isEqualTo(EstadoDeFactura.BLOQUEADA);
        assertThat(factura.numeroDeComprobante()).isNull();
    }

    @Test
    @DisplayName("FAC-05: Con incidenciasSinResolver vacia, la emision procede exitosamente")
    void fac05_permiteEmisionConIncidenciasVacias() {
        Detraccion detraccion = Detraccion.sinDetraccion("PEN");
        Factura factura = crearFactura(detraccion, Dinero.de("1000.00", "PEN"));

        factura.registrarConformidad(new Conformidad(true, List.of(), fechaBase));

        factura.emitir(NumeroDeComprobante.de("F001", 1), fechaBase);
        assertThat(factura.estado()).isEqualTo(EstadoDeFactura.EMITIDA);
    }

    @Test
    @DisplayName("Borde: emitir con fecha nula lanza IllegalArgumentException (D1)")
    void borde_emitirConFechaNulaLanzaIllegalArgumentException() {
        Detraccion detraccion = Detraccion.sinDetraccion("PEN");
        Factura factura = crearFactura(detraccion, Dinero.de("1000.00", "PEN"));
        factura.registrarConformidad(Conformidad.conforme(fechaBase));

        assertThatThrownBy(() -> factura.emitir(NumeroDeComprobante.de("F001", 1), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("La fecha de emision es obligatoria");
    }
}
