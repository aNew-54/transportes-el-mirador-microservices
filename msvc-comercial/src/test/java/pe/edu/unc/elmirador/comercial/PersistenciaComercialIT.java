package pe.edu.unc.elmirador.comercial;

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
import pe.edu.unc.elmirador.comercial.models.entity.Cliente;
import pe.edu.unc.elmirador.comercial.models.entity.ContratoMarco;
import pe.edu.unc.elmirador.comercial.models.entity.Cotizacion;
import pe.edu.unc.elmirador.comercial.models.entity.OrdenDeServicio;
import pe.edu.unc.elmirador.comercial.models.entity.PrecioDeTarifario;
import pe.edu.unc.elmirador.comercial.models.entity.TarifaPactada;
import pe.edu.unc.elmirador.comercial.models.entity.Tarifario;
import pe.edu.unc.elmirador.comercial.models.vo.Carga;
import pe.edu.unc.elmirador.comercial.models.vo.ClausulaDeConsolidacion;
import pe.edu.unc.elmirador.comercial.models.vo.CondicionDePago;
import pe.edu.unc.elmirador.comercial.models.vo.Descuento;
import pe.edu.unc.elmirador.comercial.models.vo.Dinero;
import pe.edu.unc.elmirador.comercial.models.vo.EstadoCrediticio;
import pe.edu.unc.elmirador.comercial.models.vo.EstadoDeCotizacion;
import pe.edu.unc.elmirador.comercial.models.vo.EstadoDeOrden;
import pe.edu.unc.elmirador.comercial.models.vo.ModalidadDePago;
import pe.edu.unc.elmirador.comercial.models.vo.PeriodoDeVigencia;
import pe.edu.unc.elmirador.comercial.models.vo.RazonSocial;
import pe.edu.unc.elmirador.comercial.models.vo.Recargo;
import pe.edu.unc.elmirador.comercial.models.vo.Ruc;
import pe.edu.unc.elmirador.comercial.models.vo.Ruta;
import pe.edu.unc.elmirador.comercial.models.vo.SituacionCrediticia;
import pe.edu.unc.elmirador.comercial.models.vo.Tarifa;
import pe.edu.unc.elmirador.comercial.models.vo.TiempoLibre;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeCarga;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeRecargo;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeUnidad;
import pe.edu.unc.elmirador.comercial.repositories.ClienteRepository;
import pe.edu.unc.elmirador.comercial.repositories.ContratoMarcoRepository;
import pe.edu.unc.elmirador.comercial.repositories.CotizacionRepository;
import pe.edu.unc.elmirador.comercial.repositories.OrdenDeServicioRepository;
import pe.edu.unc.elmirador.comercial.repositories.TarifarioRepository;

/**
 * Verifica las migraciones Flyway y el mapeo JPA del contexto Gestion Comercial contra un MySQL real.
 *
 * <p>Guarda los agregados completos, limpia el contexto de persistencia con {@code entityManager.clear()}
 * y los relee, verificando que los objetos de valor, colecciones embebidas y entidades hijas
 * sobrevivan al viaje de ida y vuelta.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Testcontainers
class PersistenciaComercialIT {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8.4");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CotizacionRepository cotizacionRepository;

    @Autowired
    private OrdenDeServicioRepository ordenDeServicioRepository;

    @Autowired
    private ContratoMarcoRepository contratoMarcoRepository;

    @Autowired
    private TarifarioRepository tarifarioRepository;

    private final LocalDate hoy = LocalDate.of(2026, 9, 4);

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
    void guardaYReleeClienteConSusObjetosDeValor() {
        Cliente cliente = new Cliente(
            "CLI-100",
            new Ruc("20100000001"),
            new RazonSocial("Industrias Metalicas del Norte S.A.C."),
            CondicionDePago.credito(30),
            EstadoCrediticio.vigente(hoy)
        );
        clienteRepository.saveAndFlush(cliente);
        entityManager.clear();

        Cliente leido = clienteRepository.findById("CLI-100").orElseThrow();

        assertThat(leido.id()).isEqualTo("CLI-100");
        assertThat(leido.ruc().valor()).isEqualTo("20100000001");
        assertThat(leido.razonSocial().valor()).isEqualTo("Industrias Metalicas del Norte S.A.C.");
        assertThat(leido.condicionHabitual().esACredito()).isTrue();
        assertThat(leido.condicionHabitual().plazoEnDias()).isEqualTo(30);
        assertThat(leido.estadoCrediticio().situacion()).isEqualTo(SituacionCrediticia.VIGENTE);
        assertThat(leido.estadoCrediticio().fechaDeCambio()).isEqualTo(hoy);
        assertThat(leido.puedeContratarACredito()).isTrue();
        assertThat(leido.puedeContratarAlContado()).isTrue();
    }

    @Test
    void guardaYReleeCotizacionConTarifaDosRecargosYDescuentoYTotalExacto() {
        Carga carga = new Carga(12000, new BigDecimal("45.50"), TipoDeCarga.GENERAL);
        Ruta ruta = new Ruta("LIMA", "TRUJILLO", "PANAMERICANA_NORTE");
        Dinero base = Dinero.de("1000.00", "PEN");
        Recargo r1 = new Recargo(TipoDeRecargo.COMBUSTIBLE, new BigDecimal("10.00"));
        Recargo r2 = new Recargo(TipoDeRecargo.PELIGROSIDAD, new BigDecimal("5.00"));
        Descuento descuento = new Descuento(new BigDecimal("10.00"), "GERENCIA_COMERCIAL");

        Tarifa tarifa = new Tarifa(base, List.of(r1, r2), descuento);
        Cotizacion cotizacion = Cotizacion.emitir(
            "COT-200",
            "CLI-100",
            "TAR-100",
            carga,
            ruta,
            tarifa,
            PeriodoDeVigencia.de(hoy, 7)
        );

        cotizacionRepository.saveAndFlush(cotizacion);
        entityManager.clear();

        Cotizacion leida = cotizacionRepository.findById("COT-200").orElseThrow();

        assertThat(leida.id()).isEqualTo("COT-200");
        assertThat(leida.clienteId()).isEqualTo("CLI-100");
        assertThat(leida.tarifarioId()).isEqualTo("TAR-100");
        assertThat(leida.carga().pesoKg()).isEqualTo(12000);
        assertThat(leida.carga().volumenM3()).isEqualByComparingTo(new BigDecimal("45.50"));
        assertThat(leida.carga().tipo()).isEqualTo(TipoDeCarga.GENERAL);
        assertThat(leida.ruta().origen()).isEqualTo("LIMA");
        assertThat(leida.ruta().destino()).isEqualTo("TRUJILLO");
        assertThat(leida.ruta().corredor()).isEqualTo("PANAMERICANA_NORTE");
        assertThat(leida.estado()).isEqualTo(EstadoDeCotizacion.EMITIDA);

        // Comprobar que los dos recargos vuelven intactos
        assertThat(leida.tarifa().recargos()).hasSize(2);
        assertThat(leida.tarifa().recargos())
            .extracting(Recargo::tipo)
            .containsExactlyInAnyOrder(TipoDeRecargo.COMBUSTIBLE, TipoDeRecargo.PELIGROSIDAD);

        // Comprobar que el descuento vuelve
        assertThat(leida.tarifa().descuento()).isNotNull();
        assertThat(leida.tarifa().descuento().porcentaje()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(leida.tarifa().descuento().autorizadoPor()).isEqualTo("GERENCIA_COMERCIAL");

        // Comprobar importes exactos calculados normativamente:
        // subtotal = 1000 + 100 + 50 = 1150.00
        // descuento = 1150 * 0.10 = 115.00
        // total = 1150 - 115 = 1035.00
        assertThat(leida.tarifa().subtotal().monto()).isEqualByComparingTo(new BigDecimal("1150.00"));
        assertThat(leida.tarifa().total().monto()).isEqualByComparingTo(new BigDecimal("1035.00"));
        assertThat(leida.tarifa().total().codigoMoneda()).isEqualTo("PEN");
    }

    @Test
    void guardaYReleeContratoMarcoConClausulaConsolidacionYTarifasPactadas() {
        ClausulaDeConsolidacion clausula = ClausulaDeConsolidacion.permitida(
            List.of("PANAMERICANA_NORTE", "CENTRAL")
        );
        Ruta ruta1 = new Ruta("LIMA", "PIURA", "PANAMERICANA_NORTE");
        Ruta ruta2 = new Ruta("LIMA", "AREQUIPA", "PANAMERICANA_SUR");

        TarifaPactada tp1 = new TarifaPactada("TP-301", ruta1, TipoDeUnidad.FURGON, Dinero.de("2500.00", "PEN"));
        TarifaPactada tp2 = new TarifaPactada("TP-302", ruta2, TipoDeUnidad.PLATAFORMA, Dinero.de("3200.00", "PEN"));

        ContratoMarco contrato = new ContratoMarco(
            "CTM-300",
            "CLI-100",
            new PeriodoDeVigencia(hoy, hoy.plusYears(1)),
            new TiempoLibre(4),
            clausula,
            List.of(tp1, tp2)
        );

        contratoMarcoRepository.saveAndFlush(contrato);
        entityManager.clear();

        ContratoMarco leido = contratoMarcoRepository.findById("CTM-300").orElseThrow();

        assertThat(leido.id()).isEqualTo("CTM-300");
        assertThat(leido.tiempoLibre().horas()).isEqualTo(4);

        // Comprobar que las restricciones de consolidacion vuelven
        assertThat(leido.clausulaDeConsolidacion().permitida()).isTrue();
        assertThat(leido.clausulaDeConsolidacion().restricciones())
            .hasSize(2)
            .containsExactlyInAnyOrder("PANAMERICANA_NORTE", "CENTRAL");

        // Comprobar comportamiento de negocio asociado
        assertThat(leido.obligaAConsolidar()).isTrue();
        assertThat(leido.admiteConsolidacionDe(ruta1)).isFalse();
        assertThat(leido.admiteConsolidacionDe(ruta2)).isTrue();

        // Comprobar que las dos tarifas pactadas vuelven
        assertThat(leido.tarifasPactadas()).hasSize(2);
        assertThat(leido.tarifaPara(ruta1, TipoDeUnidad.FURGON, hoy))
            .isPresent()
            .contains(Dinero.de("2500.00", "PEN"));
        assertThat(leido.tarifaPara(ruta2, TipoDeUnidad.PLATAFORMA, hoy))
            .isPresent()
            .contains(Dinero.de("3200.00", "PEN"));
    }

    @Test
    void compruebaOrphanRemovalEnContratoMarco() {
        Ruta ruta = new Ruta("LIMA", "CHICLAYO", "PANAMERICANA_NORTE");
        TarifaPactada tp1 = new TarifaPactada("TP-ORPHAN-1", ruta, TipoDeUnidad.FURGON, Dinero.de("1800.00", "PEN"));
        TarifaPactada tp2 = new TarifaPactada("TP-ORPHAN-2", ruta, TipoDeUnidad.PLATAFORMA, Dinero.de("2100.00", "PEN"));

        ContratoMarco contrato = new ContratoMarco(
            "CTM-ORPHAN",
            "CLI-100",
            new PeriodoDeVigencia(hoy, hoy.plusYears(1)),
            new TiempoLibre(2),
            ClausulaDeConsolidacion.noPermitida(),
            List.of(tp1, tp2)
        );

        contratoMarcoRepository.saveAndFlush(contrato);
        entityManager.clear();

        Long antes = (Long) entityManager
            .createQuery("select count(t) from TarifaPactada t where t.id in ('TP-ORPHAN-1', 'TP-ORPHAN-2')")
            .getSingleResult();
        assertThat(antes).isEqualTo(2L);

        ContratoMarco leido = contratoMarcoRepository.findById("CTM-ORPHAN").orElseThrow();
        contratoMarcoRepository.delete(leido);
        contratoMarcoRepository.flush();

        Long despues = (Long) entityManager
            .createQuery("select count(t) from TarifaPactada t where t.id in ('TP-ORPHAN-1', 'TP-ORPHAN-2')")
            .getSingleResult();
        assertThat(despues)
            .as("orphanRemoval: las tarifas pactadas pertenecen a ContratoMarco y no sobreviven a su raiz")
            .isZero();
    }

    @Test
    void compruebaOrphanRemovalEnTarifario() {
        Ruta ruta = new Ruta("LIMA", "CUSCO", "SUR_ORIENTAL");
        PrecioDeTarifario p1 = new PrecioDeTarifario("PT-ORPHAN-1", ruta, TipoDeUnidad.FURGON, Dinero.de("3000.00", "PEN"));
        PrecioDeTarifario p2 = new PrecioDeTarifario("PT-ORPHAN-2", ruta, TipoDeUnidad.CAMA_BAJA, Dinero.de("4500.00", "PEN"));
        Recargo r1 = new Recargo(TipoDeRecargo.ZONA_DIFICIL, new BigDecimal("15.00"));

        Tarifario tarifario = new Tarifario(
            "TAR-ORPHAN",
            new PeriodoDeVigencia(hoy, hoy.plusMonths(6)),
            List.of(p1, p2),
            List.of(r1)
        );

        tarifarioRepository.saveAndFlush(tarifario);
        entityManager.clear();

        Long antes = (Long) entityManager
            .createQuery("select count(p) from PrecioDeTarifario p where p.id in ('PT-ORPHAN-1', 'PT-ORPHAN-2')")
            .getSingleResult();
        assertThat(antes).isEqualTo(2L);

        Tarifario leido = tarifarioRepository.findById("TAR-ORPHAN").orElseThrow();
        assertThat(leido.recargosEstandar()).hasSize(1);

        tarifarioRepository.delete(leido);
        tarifarioRepository.flush();

        Long despues = (Long) entityManager
            .createQuery("select count(p) from PrecioDeTarifario p where p.id in ('PT-ORPHAN-1', 'PT-ORPHAN-2')")
            .getSingleResult();
        assertThat(despues)
            .as("orphanRemoval: los precios de tarifario pertenecen a Tarifario y no sobreviven a su raiz")
            .isZero();
    }

    @Test
    void guardaYReleeOrdenDeServicioConTarifaYCondicionDePago() {
        Carga carga = new Carga(8000, new BigDecimal("25.00"), TipoDeCarga.PALETIZADA);
        Ruta ruta = new Ruta("LIMA", "ICA", "PANAMERICANA_SUR");
        Tarifa tarifa = new Tarifa(Dinero.de("1500.00", "PEN"));
        CondicionDePago condicion = CondicionDePago.credito(15);

        OrdenDeServicio orden = OrdenDeServicio.crear(
            "ORD-500",
            "CLI-100",
            "CTM-300",
            carga,
            ruta,
            tarifa,
            condicion,
            EstadoCrediticio.vigente(hoy)
        );

        ordenDeServicioRepository.saveAndFlush(orden);
        entityManager.clear();

        OrdenDeServicio leida = ordenDeServicioRepository.findById("ORD-500").orElseThrow();

        assertThat(leida.id()).isEqualTo("ORD-500");
        assertThat(leida.clienteId()).isEqualTo("CLI-100");
        assertThat(leida.contratoId()).isEqualTo("CTM-300");
        assertThat(leida.estado()).isEqualTo(EstadoDeOrden.BORRADOR);

        // Comprobar Tarifa
        assertThat(leida.tarifa().base().monto()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(leida.tarifa().base().codigoMoneda()).isEqualTo("PEN");
        assertThat(leida.tarifa().recargos()).isEmpty();
        assertThat(leida.tarifa().descuento()).isNull();
        assertThat(leida.tarifa().total().monto()).isEqualByComparingTo(new BigDecimal("1500.00"));

        // Comprobar CondicionDePago
        assertThat(leida.condicionDePago().esACredito()).isTrue();
        assertThat(leida.condicionDePago().modalidad()).isEqualTo(ModalidadDePago.CREDITO);
        assertThat(leida.condicionDePago().plazoEnDias()).isEqualTo(15);

        // Comprobar falso flete nulo inicialmente
        assertThat(leida.falsoFlete()).isNull();
    }

    @Test
    void ordenCanceladaDespuesDelDespachoPersisteFalsoFleteYAutorizadoPor() {
        Carga carga = new Carga(5000, new BigDecimal("15.00"), TipoDeCarga.GENERAL);
        Ruta ruta = new Ruta("LIMA", "HUANCAYO", "CENTRAL");
        Tarifa tarifa = new Tarifa(Dinero.de("2000.00", "PEN"));

        OrdenDeServicio orden = OrdenDeServicio.crear(
            "ORD-CANCEL-1",
            "CLI-100",
            null,
            carga,
            ruta,
            tarifa,
            CondicionDePago.contado(),
            EstadoCrediticio.vigente(hoy)
        );
        orden.confirmar();
        orden.marcarProgramada();
        orden.marcarDespachada();
        orden.cancelar(hoy, "GERENCIA_OPERACIONES");

        ordenDeServicioRepository.saveAndFlush(orden);
        entityManager.clear();

        OrdenDeServicio leida = ordenDeServicioRepository.findById("ORD-CANCEL-1").orElseThrow();

        assertThat(leida.estado()).isEqualTo(EstadoDeOrden.CANCELADA);
        assertThat(leida.canceladoPor()).isEqualTo("GERENCIA_OPERACIONES");
        assertThat(leida.falsoFlete()).isNotNull();
        assertThat(leida.falsoFlete().base().monto()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(leida.falsoFlete().total().monto()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    void consultasDerivadasAtraviesanObjetosDeValor() {
        Cliente cliente = new Cliente(
            "CLI-DERIV-1",
            new Ruc("20999888777"),
            new RazonSocial("Distribuidora Global S.A."),
            CondicionDePago.contado(),
            EstadoCrediticio.vigente(hoy)
        );
        clienteRepository.saveAndFlush(cliente);

        ContratoMarco contrato = new ContratoMarco(
            "CTM-DERIV-1",
            "CLI-DERIV-1",
            new PeriodoDeVigencia(hoy, hoy.plusYears(1)),
            new TiempoLibre(3),
            ClausulaDeConsolidacion.permitida(List.of("SUR")),
            List.of()
        );
        contratoMarcoRepository.saveAndFlush(contrato);
        entityManager.clear();

        // Consulta derivada atravesando Ruc.valor
        Optional<Cliente> porRuc = clienteRepository.findByRucValor("20999888777");
        assertThat(porRuc).isPresent();
        assertThat(porRuc.get().id()).isEqualTo("CLI-DERIV-1");

        // Consulta derivada atravesando EstadoCrediticio.situacion
        List<Cliente> vigentes = clienteRepository.findByEstadoCrediticioSituacion(SituacionCrediticia.VIGENTE);
        assertThat(vigentes).extracting(Cliente::id).contains("CLI-DERIV-1");

        // Consulta derivada atravesando CondicionDePago.modalidad
        List<Cliente> contado = clienteRepository.findByCondicionHabitualModalidad(ModalidadDePago.CONTADO);
        assertThat(contado).extracting(Cliente::id).contains("CLI-DERIV-1");

        // Consulta derivada atravesando ClausulaDeConsolidacion.permitida
        List<ContratoMarco> conConsolidacion = contratoMarcoRepository.findByClausulaDeConsolidacionPermitida(true);
        assertThat(conConsolidacion).extracting(ContratoMarco::id).contains("CTM-DERIV-1");
    }
}
