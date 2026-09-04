package pe.edu.unc.elmirador.unidades;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import pe.edu.unc.elmirador.unidades.models.entity.DocumentoVehicular;
import pe.edu.unc.elmirador.unidades.models.entity.OrdenDeMantenimiento;
import pe.edu.unc.elmirador.unidades.models.entity.Repuesto;
import pe.edu.unc.elmirador.unidades.models.entity.TrabajoRealizado;
import pe.edu.unc.elmirador.unidades.models.entity.Unidad;
import pe.edu.unc.elmirador.unidades.models.vo.Capacidad;
import pe.edu.unc.elmirador.unidades.models.vo.Dinero;
import pe.edu.unc.elmirador.unidades.models.vo.EstadoDeOrden;
import pe.edu.unc.elmirador.unidades.models.vo.EstadoOperativo;
import pe.edu.unc.elmirador.unidades.models.vo.IntervaloDeMantenimiento;
import pe.edu.unc.elmirador.unidades.models.vo.Kilometraje;
import pe.edu.unc.elmirador.unidades.models.vo.PeriodoDeVigencia;
import pe.edu.unc.elmirador.unidades.models.vo.Placa;
import pe.edu.unc.elmirador.unidades.models.vo.ProgramaDeMantenimiento;
import pe.edu.unc.elmirador.unidades.models.vo.SituacionOperativa;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeDocumento;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeMantenimiento;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeUnidad;
import pe.edu.unc.elmirador.unidades.repositories.OrdenDeMantenimientoRepository;
import pe.edu.unc.elmirador.unidades.repositories.RepuestoRepository;
import pe.edu.unc.elmirador.unidades.repositories.UnidadRepository;

/**
 * Verifica las migraciones Flyway y el mapeo JPA del contexto Gestión de Unidades contra un MySQL real.
 *
 * <p>Es la única prueba que toca una base de datos. Con {@code ddl-auto=validate}, una entidad sin
 * su migración rompe aquí y no en producción.
 *
 * <p>El slice {@code @DataJpaTest} sólo importa Hibernate y los repositorios, de modo que Flyway se
 * añade de forma explícita.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Testcontainers
class PersistenciaUnidadesIT {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UnidadRepository unidadRepository;

    @Autowired
    private OrdenDeMantenimientoRepository ordenRepository;

    @Autowired
    private RepuestoRepository repuestoRepository;

    @Autowired
    private EntityManager entityManager;

    private final LocalDate hoy = LocalDate.of(2026, 9, 4);

    private List<DocumentoVehicular> crearCuatroDocumentosVigentes(String unidadId) {
        List<DocumentoVehicular> docs = new ArrayList<>();
        for (TipoDeDocumento tipo : TipoDeDocumento.values()) {
            docs.add(new DocumentoVehicular(
                    unidadId + "-" + tipo.name(),
                    tipo,
                    new PeriodoDeVigencia(hoy.minusMonths(6), hoy.plusMonths(6)),
                    "DOC-" + tipo.name()));
        }
        return docs;
    }

    private Unidad unidadDePrueba(String id, String placa) {
        return new Unidad(
                id,
                new Placa(placa),
                TipoDeUnidad.FURGON,
                new Capacidad(10_000, new BigDecimal("32.00")),
                new Kilometraje(10_000),
                EstadoOperativo.operativa(),
                new ProgramaDeMantenimiento(
                        new Kilometraje(10_000),
                        new Kilometraje(20_000),
                        IntervaloDeMantenimiento.ACEITE_Y_FILTROS),
                crearCuatroDocumentosVigentes(id));
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
    void guardaYReleeElAgregadoUnidadConSusCuatroDocumentosYObjetosDeValor() {
        unidadRepository.saveAndFlush(unidadDePrueba("UNI-900", "ABC-123"));
        entityManager.clear();

        Unidad leida = unidadRepository.findById("UNI-900").orElseThrow();

        assertThat(leida.getPlaca().valor()).isEqualTo("ABC-123");
        assertThat(leida.getTipo()).isEqualTo(TipoDeUnidad.FURGON);
        assertThat(leida.getCapacidad().pesoMaximoKg()).isEqualTo(10_000);
        assertThat(leida.getCapacidad().volumenMaximoM3()).isEqualByComparingTo(new BigDecimal("32.00"));
        assertThat(leida.getKilometraje().valor()).isEqualTo(10_000);
        assertThat(leida.getEstadoOperativo().situacion()).isEqualTo(SituacionOperativa.OPERATIVA);
        assertThat(leida.getEstadoOperativo().motivo()).isNull();

        assertThat(leida.getDocumentos()).hasSize(4);
        for (DocumentoVehicular doc : leida.getDocumentos()) {
            assertThat(doc.estaVigente(hoy)).isTrue();
            assertThat(doc.getVigencia().hasta()).isEqualTo(hoy.plusMonths(6));
        }
    }

    @Test
    void elEmbebidoAnidadoDeProgramaDeMantenimientoSobreviveAlViajeDeIdaYVuelta() {
        unidadRepository.saveAndFlush(unidadDePrueba("UNI-901", "ABC-124"));
        entityManager.clear();

        ProgramaDeMantenimiento programa = unidadRepository.findById("UNI-901")
                .orElseThrow()
                .getProgramaDeMantenimiento();

        assertThat(programa.kmUltimoServicio().valor())
                .as("ProgramaDeMantenimiento contiene dos Kilometraje anidados: deben sobrevivir sin chocar")
                .isEqualTo(10_000);
        assertThat(programa.kmProximoServicio().valor()).isEqualTo(20_000);
        assertThat(programa.intervalo()).isEqualTo(IntervaloDeMantenimiento.ACEITE_Y_FILTROS);
        assertThat(programa.estaVencido(new Kilometraje(20_000))).isTrue();
        assertThat(programa.requiereAlerta(new Kilometraje(19_500))).isTrue();
    }

    @Test
    void losDocumentosSonEntidadesHijasYOrphanRemovalLosEliminaConLaUnidad() {
        unidadRepository.saveAndFlush(unidadDePrueba("UNI-902", "ABC-125"));
        entityManager.clear();

        Unidad leida = unidadRepository.findById("UNI-902").orElseThrow();
        assertThat(leida.getDocumentos()).hasSize(4);

        Long antes = (Long) entityManager
                .createQuery("select count(d) from DocumentoVehicular d where d.id like 'UNI-902%'")
                .getSingleResult();
        assertThat(antes).isEqualTo(4L);

        unidadRepository.delete(leida);
        unidadRepository.flush();

        Long despues = (Long) entityManager
                .createQuery("select count(d) from DocumentoVehicular d where d.id like 'UNI-902%'")
                .getSingleResult();
        assertThat(despues)
                .as("orphanRemoval: los documentos pertenecen al agregado y no sobreviven a su raiz")
                .isZero();
    }

    @Test
    void guardaYReleeOrdenDeMantenimientoConTrabajosYCalculaCostoTotal() {
        // Guardar repuesto y unidad para satisfacer las foreign keys
        Repuesto repuesto = new Repuesto(
                "REP-900",
                "FILT-OIL-01",
                "Filtro de aceite motor",
                20,
                5,
                new Dinero(new BigDecimal("45.00"), "PEN"));
        repuestoRepository.saveAndFlush(repuesto);

        Unidad unidad = unidadDePrueba("UNI-903", "ABC-126");
        unidadRepository.saveAndFlush(unidad);

        TrabajoRealizado t1 = new TrabajoRealizado(
                "TRAB-901",
                "Cambio de aceite y filtro",
                new Dinero(new BigDecimal("120.00"), "PEN"),
                "REP-900",
                1);
        TrabajoRealizado t2 = new TrabajoRealizado(
                "TRAB-902",
                "Alineacion de direccion",
                new Dinero(new BigDecimal("80.50"), "PEN"));

        OrdenDeMantenimiento orden = new OrdenDeMantenimiento(
                "ORD-900",
                "UNI-903",
                TipoDeMantenimiento.PREVENTIVO,
                new Kilometraje(12_000),
                EstadoDeOrden.ABIERTA,
                List.of(t1, t2),
                hoy,
                null,
                "PEN");

        ordenRepository.saveAndFlush(orden);
        entityManager.clear();

        OrdenDeMantenimiento leida = ordenRepository.findById("ORD-900").orElseThrow();

        assertThat(leida.getUnidadId()).isEqualTo("UNI-903");
        assertThat(leida.getTipo()).isEqualTo(TipoDeMantenimiento.PREVENTIVO);
        assertThat(leida.getKmAtencion().valor()).isEqualTo(12_000);
        assertThat(leida.getEstado()).isEqualTo(EstadoDeOrden.ABIERTA);
        assertThat(leida.getTrabajos()).hasSize(2);
        assertThat(leida.costoTotal())
                .as("costoTotal() tras releer de BD debe sumar correctamente los costos de mano de obra")
                .isEqualTo(new Dinero(new BigDecimal("200.50"), "PEN"));
    }

    @Test
    void guardaYReleeRepuestoConSusExistenciasYDinero() {
        Repuesto repuesto = new Repuesto(
                "REP-901",
                "PAST-FR-01",
                "Pastillas de freno delanteras",
                12,
                4,
                new Dinero(new BigDecimal("185.50"), "PEN"));

        repuestoRepository.saveAndFlush(repuesto);
        entityManager.clear();

        Repuesto leido = repuestoRepository.findById("REP-901").orElseThrow();

        assertThat(leido.getCodigo()).isEqualTo("PAST-FR-01");
        assertThat(leido.getDescripcion()).isEqualTo("Pastillas de freno delanteras");
        assertThat(leido.getExistencias()).isEqualTo(12);
        assertThat(leido.getStockMinimo()).isEqualTo(4);
        assertThat(leido.getCostoUnitario().monto()).isEqualByComparingTo(new BigDecimal("185.50"));
        assertThat(leido.getCostoUnitario().codigoMoneda()).isEqualTo("PEN");
        assertThat(leido.requiereReposicion()).isFalse();
    }

    @Test
    void lasConsultasDerivadasAtraviesanLosObjetosDeValor() {
        unidadRepository.saveAndFlush(unidadDePrueba("UNI-904", "ABC-127"));
        entityManager.clear();

        Optional<Unidad> porPlaca = unidadRepository.findByPlacaValor("ABC-127");
        assertThat(porPlaca).isPresent();
        assertThat(porPlaca.get().getId()).isEqualTo("UNI-904");

        List<Unidad> operativas = unidadRepository.findByEstadoOperativoSituacion(SituacionOperativa.OPERATIVA);
        assertThat(operativas)
                .extracting(Unidad::getId)
                .contains("UNI-904");
    }
}
