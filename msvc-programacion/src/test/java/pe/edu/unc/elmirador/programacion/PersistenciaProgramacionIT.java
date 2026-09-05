package pe.edu.unc.elmirador.programacion;

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
import org.junit.jupiter.api.DisplayName;
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
import pe.edu.unc.elmirador.programacion.models.entity.AgendaDeConductor;
import pe.edu.unc.elmirador.programacion.models.entity.AgendaDeUnidad;
import pe.edu.unc.elmirador.programacion.models.entity.Viaje;
import pe.edu.unc.elmirador.programacion.models.vo.AsignacionDeRecursos;
import pe.edu.unc.elmirador.programacion.models.vo.Capacidad;
import pe.edu.unc.elmirador.programacion.models.vo.Carga;
import pe.edu.unc.elmirador.programacion.models.vo.ClausulaDeConsolidacion;
import pe.edu.unc.elmirador.programacion.models.vo.ElegibilidadDeRecurso;
import pe.edu.unc.elmirador.programacion.models.vo.EstadoDeReserva;
import pe.edu.unc.elmirador.programacion.models.vo.EstadoDeViaje;
import pe.edu.unc.elmirador.programacion.models.vo.HojaDeRuta;
import pe.edu.unc.elmirador.programacion.models.vo.Parada;
import pe.edu.unc.elmirador.programacion.models.vo.Ubicacion;
import pe.edu.unc.elmirador.programacion.models.vo.Ruta;
import pe.edu.unc.elmirador.programacion.models.vo.TipoDeCarga;
import pe.edu.unc.elmirador.programacion.models.vo.VentanaDeTiempo;
import pe.edu.unc.elmirador.programacion.repositories.AgendaDeConductorRepository;
import pe.edu.unc.elmirador.programacion.repositories.AgendaDeUnidadRepository;
import pe.edu.unc.elmirador.programacion.repositories.ViajeRepository;

/**
 * Verifica las migraciones Flyway y el mapeo JPA del contexto Programación y Despacho contra un MySQL real.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Testcontainers
class PersistenciaProgramacionIT {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ViajeRepository viajes;

    @Autowired
    private AgendaDeUnidadRepository agendasUnidades;

    @Autowired
    private AgendaDeConductorRepository agendasConductores;

    @Autowired
    private EntityManager entityManager;

    private static final ZoneOffset LIMA = ZoneOffset.ofHours(-5);

    @Test
    @DisplayName("Flyway migra el esquema y Hibernate lo valida")
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
    @DisplayName("VIA-02: Guardar Viaje con dos cargas consolidadas, clear y releer verificando peso, volumen y cabeEn")
    void guardarViajeConDosCargasYReleerloVerificandoCapacidad_VIA02() {
        Ruta ruta = new Ruta("Cajamarca", "Trujillo", "NORTE");
        VentanaDeTiempo ventana = new VentanaDeTiempo(
                OffsetDateTime.of(2026, 9, 10, 6, 0, 0, 0, LIMA),
                OffsetDateTime.of(2026, 9, 10, 18, 0, 0, 0, LIMA)
        );
        Carga carga1 = new Carga("ORD-001", 3000, new BigDecimal("10.50"), TipoDeCarga.PALETIZADA, 1);
        Viaje viaje = Viaje.planificar("VIA-100", ruta, ventana, carga1);

        Carga carga2 = new Carga("ORD-002", 4000, new BigDecimal("12.00"), TipoDeCarga.PALETIZADA, 2);
        Capacidad camion = new Capacidad(10000, new BigDecimal("30.00"));
        viaje.consolidarOrden(carga2, ruta, ventana, ClausulaDeConsolidacion.consolidacionPermitida(), camion);

        viajes.saveAndFlush(viaje);
        entityManager.clear();

        Viaje releido = viajes.findById("VIA-100").orElseThrow();

        assertThat(releido.estado()).isEqualTo(EstadoDeViaje.PLANIFICADO);
        assertThat(releido.ordenIds()).containsExactlyInAnyOrder("ORD-001", "ORD-002");
        assertThat(releido.cargaConsolidada().cargas()).hasSize(2);
        assertThat(releido.cargaConsolidada().pesoTotal()).isEqualTo(7000);
        assertThat(releido.cargaConsolidada().volumenTotal()).isEqualByComparingTo(new BigDecimal("22.50"));

        Capacidad capacidadSuficiente = new Capacidad(8000, new BigDecimal("25.00"));
        Capacidad capacidadInsuficientePeso = new Capacidad(6500, new BigDecimal("25.00"));
        Capacidad capacidadInsuficienteVolumen = new Capacidad(8000, new BigDecimal("20.00"));

        assertThat(releido.cargaConsolidada().cabeEn(capacidadSuficiente))
                .as("VIA-02: 7000 kg y 22.50 m3 cabe en 8000 kg y 25.00 m3")
                .isTrue();
        assertThat(releido.cargaConsolidada().cabeEn(capacidadInsuficientePeso))
                .as("VIA-02: 7000 kg excede 6500 kg")
                .isFalse();
        assertThat(releido.cargaConsolidada().cabeEn(capacidadInsuficienteVolumen))
                .as("VIA-02: 22.50 m3 excede 20.00 m3")
                .isFalse();
    }

    @Test
    @DisplayName("VIA-06: Guardar Viaje programado con HojaDeRuta de tres paradas de descarga y releer verificando secuenciaDeEstiba inversa")
    void guardarViajeProgramadoConHojaDeRutaYReleerloVerificandoSecuenciaDeEstiba_VIA06() {
        Ruta ruta = new Ruta("Cajamarca", "Trujillo", "NORTE");
        VentanaDeTiempo ventana = new VentanaDeTiempo(
                OffsetDateTime.of(2026, 9, 10, 6, 0, 0, 0, LIMA),
                OffsetDateTime.of(2026, 9, 10, 18, 0, 0, 0, LIMA)
        );
        Carga carga1 = new Carga("ORD-001", 3000, new BigDecimal("10.00"), TipoDeCarga.PALETIZADA, 1);
        Viaje viaje = Viaje.planificar("VIA-200", ruta, ventana, carga1);

        viaje.asignarRecursos(AsignacionDeRecursos.de("UNI-001", "CON-001"));

        HojaDeRuta hoja = HojaDeRuta.de(
                new Parada(1, Parada.CARGA, "ORD-001", Ubicacion.de("Almacen Cajamarca"), OffsetDateTime.of(2026, 9, 10, 7, 0, 0, 0, LIMA)),
                new Parada(2, Parada.DESCARGA, "ORD-001", Ubicacion.de("Almacen Chepen"), OffsetDateTime.of(2026, 9, 10, 11, 0, 0, 0, LIMA)),
                new Parada(3, Parada.DESCARGA, "ORD-002", Ubicacion.de("Almacen Pacasmayo"), OffsetDateTime.of(2026, 9, 10, 13, 0, 0, 0, LIMA)),
                new Parada(4, Parada.DESCARGA, "ORD-003", Ubicacion.de("Almacen Trujillo"), OffsetDateTime.of(2026, 9, 10, 16, 0, 0, 0, LIMA))
        );
        viaje.confirmarProgramacion(hoja);

        viajes.saveAndFlush(viaje);
        entityManager.clear();

        Viaje releido = viajes.findById("VIA-200").orElseThrow();

        assertThat(releido.estado()).isEqualTo(EstadoDeViaje.PROGRAMADO);
        assertThat(releido.asignacionDeRecursos()).isNotNull();
        assertThat(releido.asignacionDeRecursos().unidadId()).isEqualTo("UNI-001");
        assertThat(releido.asignacionDeRecursos().conductorIds()).containsExactly("CON-001");
        assertThat(releido.asignacionDeRecursos().conRelevo()).isFalse();

        assertThat(releido.hojaDeRuta()).isNotNull();
        assertThat(releido.hojaDeRuta().paradas()).hasSize(4);

        List<Parada> estiba = releido.hojaDeRuta().secuenciaDeEstiba();
        assertThat(estiba).extracting(Parada::ordenDeServicioId)
                .as("VIA-06: la secuencia de estiba devuelve las paradas de descarga en orden inverso")
                .containsExactly("ORD-003", "ORD-002", "ORD-001");
        assertThat(estiba).extracting(Parada::secuencia)
                .containsExactly(4, 3, 2);
    }

    @Test
    @DisplayName("AGU-01: Guardar AgendaDeUnidad con dos reservas y verificar que OffsetDateTime conserva su instante")
    void guardarAgendaDeUnidadConDosReservasYReleerlaVerificandoVentanasDeTiempo() {
        AgendaDeUnidad agenda = new AgendaDeUnidad("UNI-010");
        VentanaDeTiempo v1 = new VentanaDeTiempo(
                OffsetDateTime.of(2026, 9, 10, 6, 0, 0, 0, LIMA),
                OffsetDateTime.of(2026, 9, 10, 12, 0, 0, 0, LIMA)
        );
        VentanaDeTiempo v2 = new VentanaDeTiempo(
                OffsetDateTime.of(2026, 9, 10, 14, 0, 0, 0, LIMA),
                OffsetDateTime.of(2026, 9, 10, 20, 0, 0, 0, LIMA)
        );
        agenda.reservar("RES-U1", v1, ElegibilidadDeRecurso.recursoElegible(), "VIA-100");
        agenda.reservar("RES-U2", v2, ElegibilidadDeRecurso.recursoElegible(), "VIA-101");
        agenda.confirmar("RES-U1");

        agendasUnidades.saveAndFlush(agenda);
        entityManager.clear();

        AgendaDeUnidad releida = agendasUnidades.findById("UNI-010").orElseThrow();
        assertThat(releida.unidadId()).isEqualTo("UNI-010");
        assertThat(releida.reservas()).hasSize(2);
        assertThat(releida.reservasQueBloquean()).hasSize(2);

        var r1 = releida.reservas().stream().filter(r -> r.id().equals("RES-U1")).findFirst().orElseThrow();
        assertThat(r1.estado()).isEqualTo(EstadoDeReserva.CONFIRMADA);
        assertThat(r1.ventana().desde().toInstant()).isEqualTo(v1.desde().toInstant());
        assertThat(r1.ventana().hasta().toInstant()).isEqualTo(v1.hasta().toInstant());
    }

    @Test
    @DisplayName("Orphan removal en AgendaDeUnidad: eliminar la agenda borra sus reservas hijas")
    void orphanRemovalEnAgendaDeUnidad() {
        AgendaDeUnidad agenda = new AgendaDeUnidad("UNI-020");
        VentanaDeTiempo v = new VentanaDeTiempo(
                OffsetDateTime.of(2026, 9, 11, 8, 0, 0, 0, LIMA),
                OffsetDateTime.of(2026, 9, 11, 14, 0, 0, 0, LIMA)
        );
        agenda.reservar("RES-U3", v, ElegibilidadDeRecurso.recursoElegible(), "VIA-102");
        agendasUnidades.saveAndFlush(agenda);
        entityManager.clear();

        AgendaDeUnidad leida = agendasUnidades.findById("UNI-020").orElseThrow();
        agendasUnidades.delete(leida);
        agendasUnidades.flush();
        entityManager.clear();

        Long totalReservas = (Long) entityManager.createQuery("select count(r) from ReservaDeUnidad r where r.id = 'RES-U3'").getSingleResult();
        assertThat(totalReservas)
                .as("orphanRemoval: las reservas de unidad pertenecen al agregado de agenda")
                .isZero();
    }

    @Test
    @DisplayName("Orphan removal en AgendaDeConductor: eliminar la agenda borra sus reservas hijas")
    void orphanRemovalEnAgendaDeConductor() {
        AgendaDeConductor agenda = new AgendaDeConductor("CON-020");
        VentanaDeTiempo v = new VentanaDeTiempo(
                OffsetDateTime.of(2026, 9, 11, 8, 0, 0, 0, LIMA),
                OffsetDateTime.of(2026, 9, 11, 14, 0, 0, 0, LIMA)
        );
        agenda.reservar("RES-C1", v, ElegibilidadDeRecurso.recursoElegible(), "VIA-103");
        agendasConductores.saveAndFlush(agenda);
        entityManager.clear();

        AgendaDeConductor leida = agendasConductores.findById("CON-020").orElseThrow();
        agendasConductores.delete(leida);
        agendasConductores.flush();
        entityManager.clear();

        Long totalReservas = (Long) entityManager.createQuery("select count(r) from ReservaDeConductor r where r.id = 'RES-C1'").getSingleResult();
        assertThat(totalReservas)
                .as("orphanRemoval: las reservas de conductor pertenecen al agregado de agenda")
                .isZero();
    }

    @Test
    @DisplayName("Consultas derivadas en ViajeRepository")
    void consultasDerivadasEnViajeRepository() {
        Ruta rutaNorte = new Ruta("Cajamarca", "Trujillo", "NORTE");
        VentanaDeTiempo v = new VentanaDeTiempo(
                OffsetDateTime.of(2026, 9, 12, 6, 0, 0, 0, LIMA),
                OffsetDateTime.of(2026, 9, 12, 18, 0, 0, 0, LIMA)
        );
        Carga carga = new Carga("ORD-010", 2000, new BigDecimal("8.00"), TipoDeCarga.PALETIZADA, 1);
        Viaje v1 = Viaje.planificar("VIA-301", rutaNorte, v, carga);
        v1.asignarRecursos(AsignacionDeRecursos.de("UNI-050", "CON-050"));

        Viaje v2 = Viaje.planificar("VIA-302", new Ruta("Cajamarca", "Lima", "SUR"), v, carga);

        viajes.saveAndFlush(v1);
        viajes.saveAndFlush(v2);
        entityManager.clear();

        List<Viaje> planificados = viajes.findByEstado(EstadoDeViaje.PLANIFICADO);
        assertThat(planificados).extracting(Viaje::id).contains("VIA-301", "VIA-302");

        List<Viaje> porCorredor = viajes.findByRutaCorredor("NORTE");
        assertThat(porCorredor).extracting(Viaje::id).contains("VIA-301");
        assertThat(porCorredor).extracting(Viaje::id).doesNotContain("VIA-302");

        List<Viaje> porUnidad = viajes.findByAsignacionDeRecursosUnidadId("UNI-050");
        assertThat(porUnidad).extracting(Viaje::id).contains("VIA-301");
        assertThat(porUnidad).extracting(Viaje::id).doesNotContain("VIA-302");
    }
}
