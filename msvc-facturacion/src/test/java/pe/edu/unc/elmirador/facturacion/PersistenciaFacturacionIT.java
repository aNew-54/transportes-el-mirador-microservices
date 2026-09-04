package pe.edu.unc.elmirador.facturacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
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
import pe.edu.unc.elmirador.facturacion.repositories.FacturaRepository;
import pe.edu.unc.elmirador.facturacion.repositories.NotaDeCreditoRepository;

/**
 * Verifica las migraciones Flyway y el mapeo JPA del contexto Facturación contra un MySQL real.
 *
 * <p>Este contexto concentra los dos casos que el resto no tiene: un objeto de valor que contiene
 * una colección ({@code Conformidad} y sus incidencias) y un embebido que puede ser nulo
 * ({@code NumeroDeComprobante}, que no existe mientras la factura está bloqueada). Que
 * {@code ddl-auto=validate} pase no demuestra ninguno de los dos: sólo dice que las columnas
 * cuadran. Lo que lo demuestra es leer de vuelta.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Testcontainers
class PersistenciaFacturacionIT {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private FacturaRepository facturas;

    @Autowired
    private NotaDeCreditoRepository notas;

    @Autowired
    private EntityManager entityManager;

    private static final ZoneOffset LIMA = ZoneOffset.ofHours(-5);
    private final OffsetDateTime momento = OffsetDateTime.of(2026, 9, 10, 16, 30, 0, 0, LIMA);

    private Factura facturaBloqueada(String id, String ordenId) {
        SnapshotComercial snapshot = new SnapshotComercial(
                ordenId, "CLI-0007", new Dinero(new BigDecimal("1821.60"), "PEN"), "PEN", momento);
        Detraccion detraccion = new Detraccion(
                new BigDecimal("4.00"), new Dinero(new BigDecimal("72.86"), "PEN"), "00-123-456789");
        Factura factura = Factura.abrir(id, ordenId, "CLI-0007", snapshot, detraccion);
        factura.agregarLinea(new LineaDeFactura(
                id + "-L1", ordenId, ConceptoFacturable.FLETE, "Flete Cajamarca-Trujillo",
                new Dinero(new BigDecimal("1621.60"), "PEN")));
        factura.agregarLinea(new LineaDeFactura(
                id + "-L2", ordenId, ConceptoFacturable.ESTIBA, "Estiba en origen",
                new Dinero(new BigDecimal("200.00"), "PEN")));
        return factura;
    }

    @Test
    void flywayMigraElEsquemaYHibernateLoValida() throws Exception {
        try (Connection connection = dataSource.getConnection();
             ResultSet tables = connection.getMetaData()
                     .getTables(connection.getCatalog(), null, "flyway_schema_history", null)) {
            assertTrue(tables.next(),
                    "Flyway no creó flyway_schema_history: la autoconfiguración no se aplicó "
                            + "y el esquema quedaría sin versionar.");
        }
    }

    @Test
    void unaFacturaBloqueadaSeReleeConElComprobanteNulo() {
        facturas.saveAndFlush(facturaBloqueada("FAC-800", "ORD-800"));
        entityManager.clear();

        Factura leida = facturas.findById("FAC-800").orElseThrow();

        assertThat(leida.estado()).isEqualTo(EstadoDeFactura.BLOQUEADA);
        assertThat(leida.numeroDeComprobante())
                .as("un embebido cuyas columnas son todas nulas vuelve nulo; si Hibernate intentara "
                        + "construir el record, el constructor compacto lanzaria")
                .isNull();
        assertThat(leida.total().monto()).isEqualByComparingTo(new BigDecimal("1821.60"));
        assertThat(leida.montoNeto().monto()).isEqualByComparingTo(new BigDecimal("1748.74"));
    }

    @Test
    void elObjetoDeValorConColeccionSobreviveAlViajeDeIdaYVuelta() {
        Factura factura = facturaBloqueada("FAC-801", "ORD-801");
        factura.registrarConformidad(new Conformidad(
                true, List.of("DANIO en pallet 3", "FALTANTE de 2 cajas"), momento));
        facturas.saveAndFlush(factura);
        entityManager.clear();

        Conformidad leida = facturas.findById("FAC-801").orElseThrow().conformidad();

        assertThat(leida.registrada()).isTrue();
        assertThat(leida.incidenciasSinResolver())
                .as("Conformidad es un record con una @ElementCollection dentro: si no se carga EAGER, "
                        + "Hibernate no puede llamar al constructor canonico")
                .containsExactlyInAnyOrder("DANIO en pallet 3", "FALTANTE de 2 cajas");
        assertThat(leida.bloqueaEmision())
                .as("FAC-05 sigue vigente despues de releer desde la base")
                .isTrue();
    }

    @Test
    void unaFacturaEmitidaConservaSuNumeroYSusLineas() {
        Factura factura = facturaBloqueada("FAC-802", "ORD-802");
        factura.registrarConformidad(new Conformidad(true, List.of(), momento));
        factura.emitir(NumeroDeComprobante.de("F001", 310), momento);
        facturas.saveAndFlush(factura);
        entityManager.clear();

        Factura leida = facturas.findById("FAC-802").orElseThrow();

        assertThat(leida.estado()).isEqualTo(EstadoDeFactura.EMITIDA);
        assertThat(leida.numeroDeComprobante().formateado()).isEqualTo("F001-00000310");
        assertThat(leida.lineas()).hasSize(2);
        assertThat(leida.total().monto())
                .as("total() se calcula al releer, no se persiste")
                .isEqualByComparingTo(new BigDecimal("1821.60"));
    }

    @Test
    void elAjusteDeUnaNotaDeCreditoViajaConLaFacturaYLaNotaVivePorSuCuenta() {
        Factura factura = facturaBloqueada("FAC-803", "ORD-803");
        factura.registrarConformidad(new Conformidad(true, List.of(), momento));
        factura.emitir(NumeroDeComprobante.de("F001", 311), momento);

        NotaDeCredito nota = NotaDeCredito.emitir(
                "NC-001", "FAC-803", MotivoDeAjuste.FALTANTE,
                new Dinero(new BigDecimal("300.00"), "PEN"), factura.saldoAjustable(), momento);
        factura.aplicarNotaDeCredito(nota);

        facturas.saveAndFlush(factura);
        notas.saveAndFlush(nota);
        entityManager.clear();

        Factura leida = facturas.findById("FAC-803").orElseThrow();
        assertThat(leida.ajustesAplicados()).hasSize(1);
        assertThat(leida.saldoAjustable().monto())
                .as("NCR-01 se sigue calculando tras releer: 1821.60 menos el ajuste de 300.00")
                .isEqualByComparingTo(new BigDecimal("1521.60"));

        assertThat(notas.findByFacturaId("FAC-803"))
                .as("la nota es una raiz de agregado con su propio ciclo de vida")
                .hasSize(1);
    }

    @Test
    void borrarLaFacturaNoDejaLineasHuerfanasYFacEsUnicaPorOrden() {
        Factura factura = facturaBloqueada("FAC-804", "ORD-804");
        facturas.saveAndFlush(factura);
        entityManager.clear();

        assertThat(facturas.existsByOrdenDeServicioId("ORD-804"))
                .as("FAC-02: la unicidad global la cierra el repositorio mas el indice unico")
                .isTrue();
        assertThat(facturas.findByOrdenDeServicioId("ORD-804")).isPresent();

        facturas.delete(facturas.findById("FAC-804").orElseThrow());
        facturas.flush();

        Long lineas = (Long) entityManager
                .createQuery("select count(l) from LineaDeFactura l").getSingleResult();
        assertThat(lineas)
                .as("orphanRemoval: las lineas pertenecen al agregado")
                .isZero();
    }
}
