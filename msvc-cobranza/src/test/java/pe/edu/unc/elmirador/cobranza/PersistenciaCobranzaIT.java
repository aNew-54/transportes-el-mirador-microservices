package pe.edu.unc.elmirador.cobranza;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
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
import pe.edu.unc.elmirador.cobranza.models.entity.AplicacionDePago;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaCorrienteDelCliente;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaPorCobrar;
import pe.edu.unc.elmirador.cobranza.models.entity.Pago;
import pe.edu.unc.elmirador.cobranza.models.vo.Dinero;
import pe.edu.unc.elmirador.cobranza.models.vo.EstadoCrediticio;
import pe.edu.unc.elmirador.cobranza.models.vo.MedioDePago;
import pe.edu.unc.elmirador.cobranza.models.vo.ModalidadDePago;
import pe.edu.unc.elmirador.cobranza.models.vo.SituacionCrediticia;
import pe.edu.unc.elmirador.cobranza.repositories.CuentaCorrienteDelClienteRepository;
import pe.edu.unc.elmirador.cobranza.repositories.PagoRepository;

/**
 * Verifica las migraciones Flyway y el mapeo JPA del contexto Cobranza contra un MySQL real.
 *
 * <p>Es la unica prueba que toca una base de datos. Con {@code ddl-auto=validate}, una entidad sin
 * su migracion o con columnas que no cuadran rompe aqui y no en produccion.
 *
 * <p>El slice {@code @DataJpaTest} solo importa Hibernate y los repositorios, de modo que Flyway se
 * anade de forma explicita.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Testcontainers
class PersistenciaCobranzaIT {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private CuentaCorrienteDelClienteRepository cuentaCorrienteRepositorio;

    @Autowired
    private PagoRepository pagoRepositorio;

    @Autowired
    private EntityManager entityManager;

    private final LocalDate hoy = LocalDate.of(2026, 9, 10);

    @Test
    void flywayMigraElEsquemaYHibernateLoValida() throws Exception {
        try (Connection connection = dataSource.getConnection();
             ResultSet tables = connection.getMetaData()
                     .getTables(connection.getCatalog(), null, "flyway_schema_history", null)) {
            assertTrue(tables.next(),
                    "Flyway no creo flyway_schema_history: la autoconfiguracion no se aplico "
                            + "y el esquema quedaria sin versionar.");
        }
    }

    @Test
    void guardaYReleeCuentaCorrienteConCuentasPorCobrarYVerificaDinerosYCalculos() {
        String clienteId = "CLI-0007";
        CuentaCorrienteDelCliente ccc = new CuentaCorrienteDelCliente(
                clienteId,
                EstadoCrediticio.vigente(hoy.minusDays(15))
        );

        CuentaPorCobrar cpc1 = new CuentaPorCobrar(
                "CPC-001",
                clienteId,
                "FAC-2026-000100",
                "F001-00000100",
                Dinero.de("2000.00", "PEN"),
                Dinero.de("80.00", "PEN"),
                Dinero.de("1920.00", "PEN"),
                hoy.plusDays(15),
                Dinero.de("500.00", "PEN"),
                false
        );

        CuentaPorCobrar cpc2 = new CuentaPorCobrar(
                "CPC-002",
                clienteId,
                "FAC-2026-000101",
                "F001-00000101",
                Dinero.de("1000.00", "PEN"),
                Dinero.cero("PEN"),
                Dinero.de("1000.00", "PEN"),
                hoy.plusDays(30),
                Dinero.cero("PEN"),
                false
        );

        ccc.registrarCuenta(cpc1);
        ccc.registrarCuenta(cpc2);

        cuentaCorrienteRepositorio.saveAndFlush(ccc);
        entityManager.clear();

        CuentaCorrienteDelCliente leida = cuentaCorrienteRepositorio.findById(clienteId).orElseThrow();

        assertThat(leida.clienteId()).isEqualTo(clienteId);
        assertThat(leida.estado().situacion()).isEqualTo(SituacionCrediticia.VIGENTE);
        assertThat(leida.estado().motivo()).isNull();
        assertThat(leida.estado().fechaDeCambio()).isEqualTo(hoy.minusDays(15));
        assertThat(leida.cuentas()).hasSize(2);

        CuentaPorCobrar cuenta1Leida = leida.cuentas().stream()
                .filter(c -> c.id().equals("CPC-001"))
                .findFirst()
                .orElseThrow();

        // Comprobar que los TRES Dinero de una CuentaPorCobrar vuelven con su monto y moneda correctos
        assertThat(cuenta1Leida.total().monto())
                .as("Total debe persistirse con su precision decimal")
                .isEqualByComparingTo("2000.00");
        assertThat(cuenta1Leida.total().codigoMoneda()).isEqualTo("PEN");

        assertThat(cuenta1Leida.detraccion().monto())
                .as("Detraccion debe persistirse en detraccion_monto sin chocar con total")
                .isEqualByComparingTo("80.00");
        assertThat(cuenta1Leida.detraccion().codigoMoneda()).isEqualTo("PEN");

        assertThat(cuenta1Leida.aplicado().monto())
                .as("Aplicado debe persistirse en aplicado_monto sin chocar con total")
                .isEqualByComparingTo("500.00");
        assertThat(cuenta1Leida.aplicado().codigoMoneda()).isEqualTo("PEN");

        // Comprobar que saldo() y montoNeto() se siguen calculando bien DESPUES de releer
        assertThat(cuenta1Leida.montoNeto())
                .as("montoNeto() se calcula como total - detraccion (2000 - 80 = 1920)")
                .isEqualTo(Dinero.de("1920.00", "PEN"));

        assertThat(cuenta1Leida.saldo())
                .as("saldo() se calcula como montoNeto - aplicado (1920 - 500 = 1420)")
                .isEqualTo(Dinero.de("1420.00", "PEN"));

        // Comprobar deuda total de la cuenta corriente tras releer
        assertThat(leida.deudaTotal("PEN"))
                .as("deudaTotal se calcula sumando los saldos (1420 + 1000 = 2420)")
                .isEqualTo(Dinero.de("2420.00", "PEN"));
    }

    @Test
    void orphanRemovalEliminaCuentasPorCobrarAlBorrarCuentaCorriente() {
        String clienteId = "CLI-0008";
        CuentaCorrienteDelCliente ccc = new CuentaCorrienteDelCliente(
                clienteId,
                EstadoCrediticio.vigente(hoy)
        );

        CuentaPorCobrar cpc = new CuentaPorCobrar(
                "CPC-003",
                clienteId,
                "FAC-2026-000102",
                "F001-00000102",
                Dinero.de("500.00", "PEN"),
                Dinero.cero("PEN"),
                Dinero.de("500.00", "PEN"),
                hoy.plusDays(20)
        );
        ccc.registrarCuenta(cpc);

        cuentaCorrienteRepositorio.saveAndFlush(ccc);
        entityManager.clear();

        CuentaCorrienteDelCliente leida = cuentaCorrienteRepositorio.findById(clienteId).orElseThrow();
        assertThat(leida.cuentas()).hasSize(1);

        Long antes = (Long) entityManager
                .createQuery("select count(c) from CuentaPorCobrar c where c.clienteId = :clienteId")
                .setParameter("clienteId", clienteId)
                .getSingleResult();
        assertThat(antes).isEqualTo(1L);

        cuentaCorrienteRepositorio.delete(leida);
        cuentaCorrienteRepositorio.flush();

        Long despues = (Long) entityManager
                .createQuery("select count(c) from CuentaPorCobrar c where c.clienteId = :clienteId")
                .setParameter("clienteId", clienteId)
                .getSingleResult();
        assertThat(despues)
                .as("orphanRemoval: al borrar la cuenta corriente no quedan cuentas por cobrar huerfanas")
                .isZero();
    }

    @Test
    void guardaYReleePagoConAplicacionesYCalculaMontoAplicado() {
        String pagoId = "PAG-001";
        String clienteId = "CLI-0009";

        List<AplicacionDePago> aplicacionesIniciales = List.of(
                new AplicacionDePago(pagoId + "-APP-1", "CPC-101", Dinero.de("700.00", "PEN")),
                new AplicacionDePago(pagoId + "-APP-2", "CPC-102", Dinero.de("300.00", "PEN"))
        );

        Pago pago = new Pago(
                pagoId,
                clienteId,
                Dinero.de("1200.00", "PEN"),
                MedioDePago.transferencia("OP-456789"),
                hoy,
                aplicacionesIniciales
        );

        pagoRepositorio.saveAndFlush(pago);
        entityManager.clear();

        Pago leido = pagoRepositorio.findById(pagoId).orElseThrow();

        assertThat(leido.id()).isEqualTo(pagoId);
        assertThat(leido.clienteId()).isEqualTo(clienteId);
        assertThat(leido.monto()).isEqualTo(Dinero.de("1200.00", "PEN"));
        assertThat(leido.medioDePago().modalidad()).isEqualTo(ModalidadDePago.TRANSFERENCIA);
        assertThat(leido.medioDePago().referencia()).isEqualTo("OP-456789");
        assertThat(leido.fecha()).isEqualTo(hoy);

        assertThat(leido.aplicaciones()).hasSize(2);

        AplicacionDePago app1 = leido.aplicaciones().stream()
                .filter(a -> a.id().equals(pagoId + "-APP-1"))
                .findFirst()
                .orElseThrow();
        assertThat(app1.cuentaPorCobrarId()).isEqualTo("CPC-101");
        assertThat(app1.importe()).isEqualTo(Dinero.de("700.00", "PEN"));

        // Comprobar montoAplicado() y saldoSinAplicar() tras releer de base de datos
        assertThat(leido.montoAplicado())
                .as("montoAplicado() suma los importes aplicados (700 + 300 = 1000)")
                .isEqualTo(Dinero.de("1000.00", "PEN"));

        assertThat(leido.saldoSinAplicar())
                .as("saldoSinAplicar() calcula monto - montoAplicado (1200 - 1000 = 200)")
                .isEqualTo(Dinero.de("200.00", "PEN"));

        // Comprobar orphanRemoval en Pago
        pagoRepositorio.delete(leido);
        pagoRepositorio.flush();

        Long despues = (Long) entityManager
                .createQuery("select count(a) from AplicacionDePago a where a.id like 'PAG-001%'")
                .getSingleResult();
        assertThat(despues)
                .as("orphanRemoval: al borrar el pago no quedan aplicaciones huerfanas")
                .isZero();
    }

    @Test
    void consultasDerivadasEnRepositorios() {
        String clienteA = "CLI-0010";
        String clienteB = "CLI-0011";

        CuentaCorrienteDelCliente cccVigente = new CuentaCorrienteDelCliente(
                clienteA,
                EstadoCrediticio.vigente(hoy)
        );
        CuentaCorrienteDelCliente cccSuspendida = new CuentaCorrienteDelCliente(
                clienteB,
                EstadoCrediticio.suspendido("Mora excesiva", hoy)
        );

        cuentaCorrienteRepositorio.saveAndFlush(cccVigente);
        cuentaCorrienteRepositorio.saveAndFlush(cccSuspendida);

        Pago pagoA = new Pago(
                "PAG-002",
                clienteA,
                Dinero.de("500.00", "PEN"),
                MedioDePago.efectivo(),
                hoy
        );
        Pago pagoB = new Pago(
                "PAG-003",
                clienteB,
                Dinero.de("800.00", "PEN"),
                MedioDePago.deposito("DEP-112233"),
                hoy.minusDays(1)
        );

        pagoRepositorio.saveAndFlush(pagoA);
        pagoRepositorio.saveAndFlush(pagoB);

        entityManager.clear();

        // 1. Consulta derivada en CuentaCorrienteDelClienteRepository por estado
        List<CuentaCorrienteDelCliente> vigentes = cuentaCorrienteRepositorio.findByEstadoSituacion(SituacionCrediticia.VIGENTE);
        assertThat(vigentes)
                .extracting(CuentaCorrienteDelCliente::clienteId)
                .contains(clienteA)
                .doesNotContain(clienteB);

        Optional<CuentaCorrienteDelCliente> porCliente = cuentaCorrienteRepositorio.findByClienteId(clienteA);
        assertThat(porCliente).isPresent();
        assertThat(porCliente.get().clienteId()).isEqualTo(clienteA);

        // 2. Consultas derivadas en PagoRepository por clienteId y fecha
        List<Pago> pagosClienteA = pagoRepositorio.findByClienteId(clienteA);
        assertThat(pagosClienteA)
                .extracting(Pago::id)
                .containsExactly("PAG-002");

        List<Pago> pagosHoy = pagoRepositorio.findByFecha(hoy);
        assertThat(pagosHoy)
                .extracting(Pago::id)
                .contains("PAG-002")
                .doesNotContain("PAG-003");
    }
}
