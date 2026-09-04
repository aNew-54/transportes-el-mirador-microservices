package pe.edu.unc.elmirador.ejecucion;

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
import pe.edu.unc.elmirador.ejecucion.models.entity.CheckListDeSalida;
import pe.edu.unc.elmirador.ejecucion.models.entity.EjecucionDeViaje;
import pe.edu.unc.elmirador.ejecucion.models.entity.GastoDeRuta;
import pe.edu.unc.elmirador.ejecucion.models.entity.Incidencia;
import pe.edu.unc.elmirador.ejecucion.models.entity.LiquidacionDeViaje;
import pe.edu.unc.elmirador.ejecucion.models.entity.LiquidacionDeViajeId;
import pe.edu.unc.elmirador.ejecucion.models.entity.Parada;
import pe.edu.unc.elmirador.ejecucion.models.vo.ConceptoDeGasto;
import pe.edu.unc.elmirador.ejecucion.models.vo.Comprobante;
import pe.edu.unc.elmirador.ejecucion.models.vo.Dinero;
import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoDeEjecucion;
import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoDeLiquidacion;
import pe.edu.unc.elmirador.ejecucion.models.vo.Evidencia;
import pe.edu.unc.elmirador.ejecucion.models.vo.ResultadoDeCheckList;
import pe.edu.unc.elmirador.ejecucion.models.vo.SignoDeSaldo;
import pe.edu.unc.elmirador.ejecucion.models.vo.TipoDeIncidencia;
import pe.edu.unc.elmirador.ejecucion.repositories.EjecucionDeViajeRepository;
import pe.edu.unc.elmirador.ejecucion.repositories.LiquidacionDeViajeRepository;

/**
 * Verifica las migraciones Flyway y el mapeo JPA del contexto Ejecución contra un MySQL real.
 *
 * <p>Es el contexto con los mapeos más incómodos del sistema: dos objetos de valor que poseen
 * colecciones, una parada sin identidad de negocio propia y una liquidación con clave compuesta
 * porque en un viaje con relevo hay dos, una por conductor.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Testcontainers
class PersistenciaEjecucionIT {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EjecucionDeViajeRepository ejecuciones;

    @Autowired
    private LiquidacionDeViajeRepository liquidaciones;

    @Autowired
    private EntityManager entityManager;

    private static final ZoneOffset LIMA = ZoneOffset.ofHours(-5);
    private final OffsetDateTime momento = OffsetDateTime.of(2026, 9, 10, 6, 30, 0, 0, LIMA);

    private EjecucionDeViaje ejecucionDeTresParadas(String viajeId) {
        return EjecucionDeViaje.crear(viajeId, "UNI-004", List.of(
                new Parada(1, "ORD-001", "Jr. Ayacucho 450, Cajamarca"),
                new Parada(2, "ORD-002", "Av. España 1200, Trujillo"),
                new Parada(3, "ORD-003", "Av. Larco 300, Trujillo")));
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
    void laEjecucionSeReleeConSusParadasEnOrden() {
        ejecuciones.saveAndFlush(ejecucionDeTresParadas("VIA-900"));
        entityManager.clear();

        EjecucionDeViaje leida = ejecuciones.findById("VIA-900").orElseThrow();

        assertThat(leida.getEstado()).isEqualTo(EstadoDeEjecucion.PENDIENTE);
        assertThat(leida.getParadas())
                .as("@OrderBy: la secuencia de paradas no puede depender del orden de inserción")
                .extracting(Parada::getSecuencia)
                .containsExactly(1, 2, 3);
        assertThat(leida.getParadas()).extracting(Parada::getOrdenDeServicioId)
                .containsExactly("ORD-001", "ORD-002", "ORD-003");
    }

    @Test
    void elCheckListYSusObservacionesSobrevivenAlViajeDeIdaYVuelta() {
        EjecucionDeViaje ejecucion = ejecucionDeTresParadas("VIA-901");
        ejecucion.registrarCheckList(ResultadoDeCheckList.noAprobado(
                List.of("Luz de freno derecha fundida", "Extintor vencido"), momento));
        ejecuciones.saveAndFlush(ejecucion);
        entityManager.clear();

        CheckListDeSalida leido = ejecuciones.findById("VIA-901").orElseThrow().getCheckList();

        assertThat(leido.getResultado().aprobado()).isFalse();
        assertThat(leido.getResultado().observaciones())
                .as("ResultadoDeCheckList posee una colección: por eso es clase inmutable y no record")
                .containsExactlyInAnyOrder("Luz de freno derecha fundida", "Extintor vencido");
    }

    @Test
    void laEvidenciaConSusFotografiasSobreviveAlViajeDeIdaYVuelta() {
        EjecucionDeViaje ejecucion = ejecucionDeTresParadas("VIA-902");
        ejecucion.registrarCheckList(ResultadoDeCheckList.aprobado(momento));
        ejecucion.iniciar(momento);
        ejecucion.registrarIncidencia(new Incidencia(
                "INC-1", TipoDeIncidencia.DANIO, "Pallet 3 con esquina rota",
                new Evidencia(List.of("foto-1.jpg", "foto-2.jpg"), "Daño visible en esquina", momento),
                momento));
        ejecuciones.saveAndFlush(ejecucion);
        entityManager.clear();

        Incidencia leida = ejecuciones.findById("VIA-902").orElseThrow().getIncidencias().get(0);

        assertThat(leida.getTipo()).isEqualTo(TipoDeIncidencia.DANIO);
        assertThat(leida.getEvidencia().fotografias())
                .as("Evidencia posee una colección: mismo motivo que ResultadoDeCheckList")
                .containsExactlyInAnyOrder("foto-1.jpg", "foto-2.jpg");
        assertThat(leida.getEvidencia().descripcion()).isEqualTo("Daño visible en esquina");
    }

    @Test
    void elTransbordoConservaElViajeYApilaLaUnidadAnterior() {
        EjecucionDeViaje ejecucion = ejecucionDeTresParadas("VIA-903");
        ejecucion.registrarCheckList(ResultadoDeCheckList.aprobado(momento));
        ejecucion.iniciar(momento);
        ejecucion.transbordar("UNI-011");
        ejecuciones.saveAndFlush(ejecucion);
        entityManager.clear();

        EjecucionDeViaje leida = ejecuciones.findById("VIA-903").orElseThrow();

        assertThat(leida.getViajeId())
                .as("EJV-05: el transbordo no crea una ejecución nueva")
                .isEqualTo("VIA-903");
        assertThat(leida.getUnidadEjecutoraId()).isEqualTo("UNI-011");
        assertThat(leida.getUnidadesAnteriores()).containsExactly("UNI-004");
    }

    @Test
    void borrarLaEjecucionNoDejaParadasHuerfanas() {
        ejecuciones.saveAndFlush(ejecucionDeTresParadas("VIA-904"));
        entityManager.clear();

        ejecuciones.delete(ejecuciones.findById("VIA-904").orElseThrow());
        ejecuciones.flush();

        Long paradas = (Long) entityManager.createQuery("select count(p) from Parada p").getSingleResult();
        assertThat(paradas)
                .as("orphanRemoval: las paradas pertenecen al agregado")
                .isZero();
    }

    @Test
    void unViajeConRelevoTieneDosLiquidacionesIndependientes() {
        LiquidacionDeViaje deElena = LiquidacionDeViaje.abrir(
                "VIA-905", "CON-011", new Dinero(new BigDecimal("800.00"), "PEN"));
        LiquidacionDeViaje deMarco = LiquidacionDeViaje.abrir(
                "VIA-905", "CON-012", new Dinero(new BigDecimal("500.00"), "PEN"));
        deElena.rendirGasto(new GastoDeRuta(
                "G-1", ConceptoDeGasto.COMBUSTIBLE, new Dinero(new BigDecimal("320.50"), "PEN"),
                new Comprobante("FACTURA", "F001-88", momento), "Grifo Repsol km 120"));
        deElena.aprobar(momento);

        liquidaciones.saveAndFlush(deElena);
        liquidaciones.saveAndFlush(deMarco);
        entityManager.clear();

        List<LiquidacionDeViaje> delViaje = liquidaciones.findByViajeId("VIA-905");
        assertThat(delViaje)
                .as("mismo viaje, dos conductores: la clave es compuesta")
                .hasSize(2);

        LiquidacionDeViaje leidaElena = liquidaciones
                .findById(new LiquidacionDeViajeId("VIA-905", "CON-011")).orElseThrow();
        assertThat(leidaElena.getEstado()).isEqualTo(EstadoDeLiquidacion.APROBADA);
        assertThat(leidaElena.getGastos()).hasSize(1);
        assertThat(leidaElena.totalDeGastos().monto()).isEqualByComparingTo(new BigDecimal("320.50"));
        assertThat(leidaElena.saldo().signo())
                .as("LIQ-02: el saldo se calcula tras releer, no se persiste")
                .isEqualTo(SignoDeSaldo.A_FAVOR_DE_LA_EMPRESA);

        assertThat(liquidaciones.findByViajeIdAndEstadoNot("VIA-905", EstadoDeLiquidacion.APROBADA))
                .as("LIQ-04: la de Marco sigue pendiente y bloquea el cierre")
                .hasSize(1);
    }

    @Test
    void laTablaDeLiquidacionesNoTieneColumnaDeSaldo() throws Exception {
        try (Connection connection = dataSource.getConnection();
             ResultSet columnas = connection.getMetaData()
                     .getColumns(connection.getCatalog(), null, "liquidaciones", "%")) {
            while (columnas.next()) {
                assertThat(columnas.getString("COLUMN_NAME").toLowerCase())
                        .as("LIQ-02: el saldo nunca se almacena, tampoco en la tabla")
                        .isNotEqualTo("saldo");
            }
        }
    }
}
