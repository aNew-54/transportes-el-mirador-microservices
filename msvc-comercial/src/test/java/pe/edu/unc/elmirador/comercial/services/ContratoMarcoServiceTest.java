package pe.edu.unc.elmirador.comercial.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.edu.unc.elmirador.comercial.dto.request.RegistrarContratoMarcoRequest;
import pe.edu.unc.elmirador.comercial.dto.response.ContratoMarcoResponse;
import pe.edu.unc.elmirador.comercial.models.entity.Cliente;
import pe.edu.unc.elmirador.comercial.models.entity.ContratoMarco;
import pe.edu.unc.elmirador.comercial.models.vo.CondicionDePago;
import pe.edu.unc.elmirador.comercial.models.vo.EstadoCrediticio;
import pe.edu.unc.elmirador.comercial.models.vo.ModalidadDePago;
import pe.edu.unc.elmirador.comercial.models.vo.RazonSocial;
import pe.edu.unc.elmirador.comercial.models.vo.Ruc;
import pe.edu.unc.elmirador.comercial.models.vo.SituacionCrediticia;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeUnidad;
import pe.edu.unc.elmirador.comercial.repositories.ClienteRepository;
import pe.edu.unc.elmirador.comercial.repositories.ContratoMarcoRepository;

class ContratoMarcoServiceTest {

    private ContratoMarcoRepository contratoRepository;
    private ClienteRepository clienteRepository;
    private Clock reloj;
    private ContratoMarcoService servicio;

    @BeforeEach
    void setUp() {
        contratoRepository = mock(ContratoMarcoRepository.class);
        clienteRepository = mock(ClienteRepository.class);
        reloj = Clock.fixed(Instant.parse("2026-09-04T10:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new ContratoMarcoService(contratoRepository, clienteRepository, reloj);
    }

    @Test
    void registrar_clienteValido_guardaYDevuelveRespuesta() {
        RegistrarContratoMarcoRequest request = new RegistrarContratoMarcoRequest(
                "cli-1", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"),
                48, true, List.of("NORTE"),
                List.of(new RegistrarContratoMarcoRequest.TarifaPactadaRequest(
                        "LIMA", "PIURA", "NORTE", TipoDeUnidad.FURGON, new BigDecimal("1000.00"), "PEN")));

        Cliente cliente = new Cliente(
                "cli-1", new Ruc("20123456789"), new RazonSocial("Acme S.A."),
                new CondicionDePago(ModalidadDePago.CONTADO, 0),
                new EstadoCrediticio(SituacionCrediticia.VIGENTE, LocalDate.now(reloj))
        );

        when(clienteRepository.findById("cli-1")).thenReturn(Optional.of(cliente));
        when(contratoRepository.save(any(ContratoMarco.class))).thenAnswer(i -> i.getArgument(0));

        ContratoMarcoResponse respuesta = servicio.registrar(request);

        assertEquals("cli-1", respuesta.clienteId());
        verify(contratoRepository).save(any(ContratoMarco.class));
    }
}
