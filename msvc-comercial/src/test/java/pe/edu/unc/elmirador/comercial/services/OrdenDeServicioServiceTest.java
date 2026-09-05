package pe.edu.unc.elmirador.comercial.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeCarga;
import pe.edu.unc.elmirador.comercial.repositories.ClienteRepository;
import pe.edu.unc.elmirador.comercial.repositories.ContratoMarcoRepository;
import pe.edu.unc.elmirador.comercial.repositories.OrdenDeServicioRepository;

class OrdenDeServicioServiceTest {

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

    @Test
    void crear_clienteValido_guardaYDevuelveRespuesta() {
        CrearOrdenRequest request = new CrearOrdenRequest(
                "cli-1", null, 1000, new BigDecimal("10.00"), TipoDeCarga.GENERAL,
                "LIMA", "PIURA", "NORTE", ModalidadDePago.CONTADO, 0);
        
        Cliente cliente = new Cliente(
                "cli-1", new Ruc("20123456789"), new RazonSocial("Acme S.A."),
                new CondicionDePago(ModalidadDePago.CONTADO, 0),
                new EstadoCrediticio(SituacionCrediticia.VIGENTE, LocalDate.now(reloj))
        );

        when(clienteRepository.findById("cli-1")).thenReturn(Optional.of(cliente));
        when(ordenRepository.save(any(OrdenDeServicio.class))).thenAnswer(i -> i.getArgument(0));

        OrdenDeServicioResponse response = servicio.crear(request);

        assertEquals("cli-1", response.clienteId());
        assertEquals("BORRADOR", response.estado());
        verify(ordenRepository).save(any(OrdenDeServicio.class));
    }

    @Test
    void crear_condicionCreditoParaClienteSuspendido_lanzaExcepcion() {
        CrearOrdenRequest request = new CrearOrdenRequest(
                "cli-1", null, 1000, new BigDecimal("10.00"), TipoDeCarga.GENERAL,
                "LIMA", "PIURA", "NORTE", ModalidadDePago.CREDITO, 30);
        
        Cliente cliente = new Cliente(
                "cli-1", new Ruc("20123456789"), new RazonSocial("Acme S.A."),
                new CondicionDePago(ModalidadDePago.CREDITO, 30),
                new EstadoCrediticio(SituacionCrediticia.SUSPENDIDO, LocalDate.now(reloj))
        );

        when(clienteRepository.findById("cli-1")).thenReturn(Optional.of(cliente));

        assertThrows(CondicionDePagoInconsistenteException.class, () -> servicio.crear(request));
    }
}
