package pe.edu.unc.elmirador.conductores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.time.LocalDate;
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
import pe.edu.unc.elmirador.conductores.models.entity.Conductor;
import pe.edu.unc.elmirador.conductores.models.entity.Induccion;
import pe.edu.unc.elmirador.conductores.models.vo.CategoriaDeLicencia;
import pe.edu.unc.elmirador.conductores.models.vo.EstadoDeHabilitacion;
import pe.edu.unc.elmirador.conductores.models.vo.HorasDeConduccion;
import pe.edu.unc.elmirador.conductores.models.vo.NumeroDeLicencia;
import pe.edu.unc.elmirador.conductores.models.vo.PeriodoDeVigencia;
import pe.edu.unc.elmirador.conductores.models.vo.SituacionDeHabilitacion;
import pe.edu.unc.elmirador.conductores.repositories.ConductorRepository;

/**
 * Verifica las migraciones Flyway y el mapeo JPA del contexto Gestión de Conductores contra un
 * MySQL real.
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
class PersistenciaConductoresIT {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ConductorRepository repositorio;

    @Autowired
    private EntityManager entityManager;

    private final LocalDate hoy = LocalDate.of(2026, 9, 4);

    private Conductor conductorDePrueba(String id, String licencia) {
        return new Conductor(
                id,
                "Elena Quiroz Malca",
                new NumeroDeLicencia(licencia),
                CategoriaDeLicencia.A_IIIC,
                new PeriodoDeVigencia(hoy.minusYears(1), hoy.plusYears(2)),
                new HorasDeConduccion(
                        new BigDecimal("4.50"),
                        new PeriodoDeVigencia(hoy, hoy.plusDays(1))),
                EstadoDeHabilitacion.habilitado(),
                List.of(new Induccion(
                        id + "-IND-1",
                        "CLI-0019",
                        new PeriodoDeVigencia(hoy.minusMonths(2), hoy.plusMonths(10)))));
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
    void guardaYReleeElAgregadoCompletoConSusObjetosDeValor() {
        repositorio.saveAndFlush(conductorDePrueba("CON-900", "Q11111111"));
        entityManager.clear();

        Conductor leido = repositorio.findById("CON-900").orElseThrow();

        assertThat(leido.getNumeroDeLicencia().valor()).isEqualTo("Q11111111");
        assertThat(leido.getCategoriaDeLicencia()).isEqualTo(CategoriaDeLicencia.A_IIIC);
        assertThat(leido.getVigenciaLicencia().hasta()).isEqualTo(hoy.plusYears(2));
        assertThat(leido.getEstado().situacion()).isEqualTo(SituacionDeHabilitacion.HABILITADO);
        assertThat(leido.getEstado().motivo()).isNull();
    }

    @Test
    void elEmbebidoAnidadoDeHorasSobreviveAlViajeDeIdaYVuelta() {
        repositorio.saveAndFlush(conductorDePrueba("CON-901", "Q22222222"));
        entityManager.clear();

        HorasDeConduccion horas = repositorio.findById("CON-901").orElseThrow().getHorasAcumuladas();

        assertThat(horas.horas())
                .as("HorasDeConduccion contiene un PeriodoDeVigencia: si el embebido anidado no se "
                        + "mapea, esto vuelve nulo o con la ventana equivocada")
                .isEqualByComparingTo(new BigDecimal("4.50"));
        assertThat(horas.ventanaDeComputo().desde()).isEqualTo(hoy);
        assertThat(horas.ventanaDeComputo().hasta()).isEqualTo(hoy.plusDays(1));
        assertThat(horas.cubre(hoy)).isTrue();
    }

    @Test
    void laInduccionEsEntidadHijaYViajaConElConductor() {
        repositorio.saveAndFlush(conductorDePrueba("CON-902", "Q33333333"));
        entityManager.clear();

        Conductor leido = repositorio.findById("CON-902").orElseThrow();

        assertThat(leido.getInducciones()).hasSize(1);
        assertThat(leido.getInducciones().get(0).getClienteId()).isEqualTo("CLI-0019");
        assertThat(leido.getInducciones().get(0).estaVigenteEn(hoy)).isTrue();

        Long antes = (Long) entityManager
                .createQuery("select count(i) from Induccion i").getSingleResult();
        assertThat(antes).isEqualTo(1L);

        repositorio.delete(leido);
        repositorio.flush();

        Long despues = (Long) entityManager
                .createQuery("select count(i) from Induccion i").getSingleResult();
        assertThat(despues)
                .as("orphanRemoval: la induccion pertenece al agregado y no sobrevive a su raiz")
                .isZero();
    }

    @Test
    void lasConsultasDerivadasAtraviesanLosObjetosDeValor() {
        repositorio.saveAndFlush(conductorDePrueba("CON-903", "Q44444444"));
        entityManager.clear();

        Optional<Conductor> porLicencia = repositorio.findByNumeroDeLicenciaValor("Q44444444");
        assertThat(porLicencia).isPresent();

        assertThat(repositorio.findByEstadoSituacion(SituacionDeHabilitacion.HABILITADO))
                .extracting(Conductor::getId)
                .contains("CON-903");
    }
}
