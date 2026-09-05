package pe.edu.unc.elmirador.comercial.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.edu.unc.elmirador.comercial.dto.request.CrearOrdenRequest;
import pe.edu.unc.elmirador.comercial.dto.response.OrdenDeServicioResponse;
import pe.edu.unc.elmirador.comercial.exceptions.CondicionDePagoInconsistenteException;
import pe.edu.unc.elmirador.comercial.models.entity.Cliente;
import pe.edu.unc.elmirador.comercial.models.entity.OrdenDeServicio;
import pe.edu.unc.elmirador.comercial.models.vo.CondicionDePago;
import pe.edu.unc.elmirador.comercial.models.vo.EstadoCrediticio;
import pe.edu.unc.elmirador.comercial.models.vo.ModalidadDePago;
import pe.edu.unc.elmirador.comercial.models.vo.RazonSocial;
import pe.edu.unc.elmirador.comercial.models.vo.Ruc;
import pe.edu.unc.elmirador.comercial.models.vo.SituacionCrediticia;
import pe.edu.unc.elmirador.comercial.models.entity.ContratoMarco;
import pe.edu.unc.elmirador.comercial.models.entity.TarifaPactada;
import pe.edu.unc.elmirador.comercial.models.vo.PeriodoDeVigencia;
import pe.edu.unc.elmirador.comercial.models.vo.TiempoLibre;
import pe.edu.unc.elmirador.comercial.models.vo.ClausulaDeConsolidacion;
import pe.edu.unc.elmirador.comercial.models.vo.Dinero;
import pe.edu.unc.elmirador.comercial.models.vo.Ruta;
import pe.edu.unc.elmirador.comercial.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeCarga;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeUnidad;
import pe.edu.unc.elmirador.comercial.repositories.ClienteRepository;
import pe.edu.unc.elmirador.comercial.repositories.ContratoMarcoRepository;
import pe.edu.unc.elmirador.comercial.repositories.OrdenDeServicioRepository;

class OrdenDeServicioServiceTest {

    private static final OffsetDateTime VENTANA_INICIO =
            OffsetDateTime.parse("2026-09-10T06:00:00-05:00");
    private static final OffsetDateTime VENTANA_FIN =
            OffsetDateTime.parse("2026-09-10T18:00:00-05:00");

    private OrdenDeServicioRepository ordenRepository;
    private ClienteRepository clienteRepository;
    private ContratoMarcoRepository contratoRepository;
    private Clock reloj;
    private OrdenDeServicioService servicio;

    @BeforeEach
    void setUp() {
        ordenRepository = mock(OrdenDeServicioRepository.class);
        clienteRepository = mock(ClienteRepository.class);
        contratoRepository = mock(ContratoMarcoRepository.class);
        reloj = Clock.fixed(Instant.parse("2026-09-04T10:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new OrdenDeServicioService(ordenRepository, clienteRepository, contratoRepository, reloj);
    }

    /** Contrato vigente con una tarifa pactada de 100 PEN para LIMA-PIURA en furgon. */
    private ContratoMarco contratoConTarifaPactada() {
        return new ContratoMarco(
                "ctm-1", "cli-1",
                new PeriodoDeVigencia(LocalDate.now(reloj).minusMonths(1), LocalDate.now(reloj).plusMonths(11)),
                new TiempoLibre(4),
                new ClausulaDeConsolidacion(true, java.util.List.of()),
                java.util.List.of(new TarifaPactada(
                        "tp-1", new Ruta("LIMA", "PIURA", "NORTE"), TipoDeUnidad.FURGON,
                        new Dinero(new BigDecimal("100.00"), "PEN"))));
    }

    @Test
    void crear_clienteValido_guardaYDevuelveRespuesta() {
        CrearOrdenRequest request = new CrearOrdenRequest(
                "cli-1", "ctm-1", TipoDeUnidad.FURGON, 1000, new BigDecimal("10.00"), TipoDeCarga.GENERAL,
                "LIMA", "PIURA", "NORTE",
                "PALLETS", "ALIMENTARIA", 296,
                VENTANA_INICIO, VENTANA_FIN,
                ModalidadDePago.CONTADO, 0);
        
        Cliente cliente = new Cliente(
                "cli-1", new Ruc("20123456789"), new RazonSocial("Acme S.A."),
                new CondicionDePago(ModalidadDePago.CONTADO, 0),
                new EstadoCrediticio(SituacionCrediticia.VIGENTE, LocalDate.now(reloj))
        );

        when(clienteRepository.findById("cli-1")).thenReturn(Optional.of(cliente));
        when(contratoRepository.findById("ctm-1")).thenReturn(Optional.of(contratoConTarifaPactada()));
        when(ordenRepository.save(any(OrdenDeServicio.class))).thenAnswer(i -> i.getArgument(0));

        OrdenDeServicioResponse response = servicio.crear(request);

        assertEquals("cli-1", response.clienteId());
        assertEquals("BORRADOR", response.estado());
        verify(ordenRepository).save(any(OrdenDeServicio.class));
    }

    @Test
    void crear_condicionCreditoParaClienteSuspendido_lanzaExcepcion() {
        CrearOrdenRequest request = new CrearOrdenRequest(
                "cli-1", "ctm-1", TipoDeUnidad.FURGON, 1000, new BigDecimal("10.00"), TipoDeCarga.GENERAL,
                "LIMA", "PIURA", "NORTE",
                "PALLETS", "ALIMENTARIA", 296,
                VENTANA_INICIO, VENTANA_FIN,
                ModalidadDePago.CREDITO, 30);
        
        Cliente cliente = new Cliente(
                "cli-1", new Ruc("20123456789"), new RazonSocial("Acme S.A."),
                new CondicionDePago(ModalidadDePago.CREDITO, 30),
                new EstadoCrediticio(SituacionCrediticia.SUSPENDIDO, LocalDate.now(reloj))
        );

        when(clienteRepository.findById("cli-1")).thenReturn(Optional.of(cliente));
        when(contratoRepository.findById("ctm-1")).thenReturn(Optional.of(contratoConTarifaPactada()));

        assertThrows(CondicionDePagoInconsistenteException.class, () -> servicio.crear(request));
    }

    /**
     * El precio sale de la tarifa pactada del contrato. Antes el servicio se inventaba una tarifa
     * fija de 100 PEN y esta prueba no existia, asi que nadie notaba que el importe era falso.
     */
    @Test
    void crear_tomaElPrecioDeLaTarifaPactadaDelContrato() {
        CrearOrdenRequest request = new CrearOrdenRequest(
                "cli-1", "ctm-1", TipoDeUnidad.FURGON, 1000, new BigDecimal("10.00"), TipoDeCarga.GENERAL,
                "LIMA", "PIURA", "NORTE",
                "PALLETS", "ALIMENTARIA", 296,
                VENTANA_INICIO, VENTANA_FIN,
                ModalidadDePago.CONTADO, 0);

        Cliente cliente = new Cliente(
                "cli-1", new Ruc("20123456789"), new RazonSocial("Acme S.A."),
                new CondicionDePago(ModalidadDePago.CONTADO, 0),
                new EstadoCrediticio(SituacionCrediticia.VIGENTE, LocalDate.now(reloj)));

        when(clienteRepository.findById("cli-1")).thenReturn(Optional.of(cliente));
        when(contratoRepository.findById("ctm-1")).thenReturn(Optional.of(contratoConTarifaPactada()));
        when(ordenRepository.save(any(OrdenDeServicio.class))).thenAnswer(i -> i.getArgument(0));

        OrdenDeServicioResponse respuesta = servicio.crear(request);

        assertEquals(0, new BigDecimal("100.00").compareTo(respuesta.tarifa().baseMonto()));
        assertEquals("PEN", respuesta.tarifa().baseMoneda());
    }

    /** Una ruta que el contrato no tarifica es un 404 explicito, no un importe inventado. */
    @Test
    void crear_sinTarifaPactadaParaEsaRuta_es404() {
        CrearOrdenRequest request = new CrearOrdenRequest(
                "cli-1", "ctm-1", TipoDeUnidad.CAMA_BAJA, 1000, new BigDecimal("10.00"), TipoDeCarga.GENERAL,
                "LIMA", "PIURA", "NORTE",
                "PALLETS", "ALIMENTARIA", 296,
                VENTANA_INICIO, VENTANA_FIN,
                ModalidadDePago.CONTADO, 0);

        Cliente cliente = new Cliente(
                "cli-1", new Ruc("20123456789"), new RazonSocial("Acme S.A."),
                new CondicionDePago(ModalidadDePago.CONTADO, 0),
                new EstadoCrediticio(SituacionCrediticia.VIGENTE, LocalDate.now(reloj)));

        when(clienteRepository.findById("cli-1")).thenReturn(Optional.of(cliente));
        when(contratoRepository.findById("ctm-1")).thenReturn(Optional.of(contratoConTarifaPactada()));

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.crear(request));
    }
}
