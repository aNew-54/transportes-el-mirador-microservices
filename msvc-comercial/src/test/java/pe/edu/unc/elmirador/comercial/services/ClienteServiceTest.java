package pe.edu.unc.elmirador.comercial.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.edu.unc.elmirador.comercial.dto.request.RegistrarClienteRequest;
import pe.edu.unc.elmirador.comercial.dto.response.ClienteResponse;
import pe.edu.unc.elmirador.comercial.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.comercial.models.entity.Cliente;
import pe.edu.unc.elmirador.comercial.models.vo.CondicionDePago;
import pe.edu.unc.elmirador.comercial.models.vo.EstadoCrediticio;
import pe.edu.unc.elmirador.comercial.models.vo.ModalidadDePago;
import pe.edu.unc.elmirador.comercial.models.vo.RazonSocial;
import pe.edu.unc.elmirador.comercial.models.vo.Ruc;
import pe.edu.unc.elmirador.comercial.models.vo.SituacionCrediticia;
import pe.edu.unc.elmirador.comercial.repositories.ClienteRepository;

class ClienteServiceTest {

    private ClienteRepository repositorio;
    private Clock reloj;
    private ClienteService servicio;

    @BeforeEach
    void setUp() {
        repositorio = mock(ClienteRepository.class);
        reloj = Clock.fixed(Instant.parse("2026-09-04T10:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new ClienteService(repositorio, reloj);
    }

    @Test
    void registrar_clienteValido_guardaYDevuelveRespuesta() {
        RegistrarClienteRequest peticion = new RegistrarClienteRequest(
                "20123456789", "Acme S.A.", ModalidadDePago.CREDITO, 30);
        
        when(repositorio.findByRucValor("20123456789")).thenReturn(Optional.empty());
        when(repositorio.save(any(Cliente.class))).thenAnswer(i -> {
            Cliente c = i.getArgument(0);
            return new Cliente(
                    "cli-123", c.ruc(), c.razonSocial(), c.condicionHabitual(), c.estadoCrediticio());
        });

        ClienteResponse respuesta = servicio.registrar(peticion);

        assertEquals("cli-123", respuesta.id());
        assertEquals("20123456789", respuesta.ruc());
        verify(repositorio).save(any(Cliente.class));
    }

    @Test
    void registrar_rucDuplicado_lanzaConflicto() {
        RegistrarClienteRequest peticion = new RegistrarClienteRequest(
                "20123456789", "Acme S.A.", ModalidadDePago.CREDITO, 30);
        
        Cliente existente = new Cliente(
                "cli-999", new Ruc("20123456789"), new RazonSocial("Otra S.A."),
                new CondicionDePago(ModalidadDePago.CONTADO, 0),
                new EstadoCrediticio(SituacionCrediticia.VIGENTE, java.time.LocalDate.now(reloj)));
        
        when(repositorio.findByRucValor("20123456789")).thenReturn(Optional.of(existente));

        assertThrows(ConflictoDeRecursoException.class, () -> servicio.registrar(peticion));
    }
}
