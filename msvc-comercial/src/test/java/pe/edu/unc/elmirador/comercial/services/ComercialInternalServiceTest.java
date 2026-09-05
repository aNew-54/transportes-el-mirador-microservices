package pe.edu.unc.elmirador.comercial.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pe.edu.unc.elmirador.comercial.dto.internal.request.DiferenciaDeCargaRequest;
import pe.edu.unc.elmirador.comercial.dto.internal.request.EsperaRequest;
import pe.edu.unc.elmirador.comercial.models.entity.Cliente;
import pe.edu.unc.elmirador.comercial.models.entity.OrdenDeServicio;
import pe.edu.unc.elmirador.comercial.models.entity.PeticionIdempotente;
import pe.edu.unc.elmirador.comercial.models.vo.Carga;
import pe.edu.unc.elmirador.comercial.models.vo.CondicionDePago;
import pe.edu.unc.elmirador.comercial.models.vo.Dinero;
import pe.edu.unc.elmirador.comercial.models.vo.EstadoCrediticio;
import pe.edu.unc.elmirador.comercial.models.vo.EstadoDeOrden;
import pe.edu.unc.elmirador.comercial.models.vo.ModalidadDePago;
import pe.edu.unc.elmirador.comercial.models.vo.RazonSocial;
import pe.edu.unc.elmirador.comercial.models.vo.Ruc;
import pe.edu.unc.elmirador.comercial.models.vo.Ruta;
import pe.edu.unc.elmirador.comercial.models.vo.SituacionCrediticia;
import pe.edu.unc.elmirador.comercial.models.vo.Tarifa;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeCarga;
import pe.edu.unc.elmirador.comercial.repositories.ClienteRepository;
import pe.edu.unc.elmirador.comercial.repositories.ContratoMarcoRepository;
import pe.edu.unc.elmirador.comercial.repositories.OrdenDeServicioRepository;
import pe.edu.unc.elmirador.comercial.repositories.PeticionIdempotenteRepository;

class ComercialInternalServiceTest {

    private OrdenDeServicioRepository ordenRepository;
    private ClienteRepository clienteRepository;
    private ContratoMarcoRepository contratoRepository;
    private PeticionIdempotenteRepository idempotencia;
    private ComercialInternalService servicio;
    private Clock reloj;

    @BeforeEach
    void preparar() {
        ordenRepository = mock(OrdenDeServicioRepository.class);
        clienteRepository = mock(ClienteRepository.class);
        contratoRepository = mock(ContratoMarcoRepository.class);
        idempotencia = mock(PeticionIdempotenteRepository.class);
        reloj = Clock.fixed(OffsetDateTime.parse("2026-09-10T16:00:00-05:00").toInstant(), ZoneId.of("America/Lima"));
        servicio = new ComercialInternalService(ordenRepository, clienteRepository, contratoRepository, idempotencia, reloj);
    }

    private OrdenDeServicio orden() {
        return new OrdenDeServicio(
                "ORD-2026-000123", "CLI-0007", null,
                new Carga(8500, new BigDecimal("24.5"), TipoDeCarga.GENERAL),
                new Ruta("Cajamarca", "Trujillo", "COSTA_NORTE"),
                new Tarifa(new Dinero(new BigDecimal("1800.00"), "PEN")),
                new CondicionDePago(ModalidadDePago.CREDITO, 30),
                EstadoDeOrden.CONFIRMADA, null
        );
    }

    private Cliente cliente() {
        return new Cliente("CLI-0007", new Ruc("20481234567"), new RazonSocial("Distribuidora Norte S.A.C."),
                new CondicionDePago(ModalidadDePago.CREDITO, 30),
                new EstadoCrediticio(SituacionCrediticia.VIGENTE, LocalDate.now(reloj)));
    }

    @Test
    @DisplayName("consultar snapshot facturable funciona y mapea los datos")
    void consultarSnapshot() {
        when(ordenRepository.findById("ORD-2026-000123")).thenReturn(Optional.of(orden()));
        when(clienteRepository.findById("CLI-0007")).thenReturn(Optional.of(cliente()));

        var respuesta = servicio.consultarSnapshotFacturable("ORD-2026-000123");
        
        assertThat(respuesta.ordenId()).isEqualTo("ORD-2026-000123");
        assertThat(respuesta.clienteId()).isEqualTo("CLI-0007");
        assertThat(respuesta.ruc()).isEqualTo("20481234567");
        assertThat(respuesta.razonSocial()).isEqualTo("Distribuidora Norte S.A.C.");
    }

    @Test
    @DisplayName("idempotencia: reintento de diferencia de carga devuelve el mismo resultado y no guarda otra vez")
    void reintentoDiferencia() {
        when(ordenRepository.findById("ORD-1")).thenReturn(Optional.of(orden()));
        String clave = "k-diff";
        DiferenciaDeCargaRequest pet = new DiferenciaDeCargaRequest(
                "VIA-1", new DiferenciaDeCargaRequest.CargaInfo(1, BigDecimal.ONE, "s"),
                new DiferenciaDeCargaRequest.CargaInfo(2, BigDecimal.ONE, "s"), "ACEPTADA", OffsetDateTime.now());

        var res1 = servicio.reportarDiferencia("ORD-1", clave, pet);
        assertThat(res1.repetida()).isFalse();

        when(idempotencia.findById(clave)).thenReturn(Optional.of(new PeticionIdempotente(clave, "ORD-1", OffsetDateTime.now())));

        var res2 = servicio.reportarDiferencia("ORD-1", clave, pet);
        assertThat(res2.repetida()).isTrue();
        verify(idempotencia, times(1)).save(any(PeticionIdempotente.class));
    }

    @Test
    @DisplayName("idempotencia: reintento de esperas devuelve el mismo resultado y no guarda otra vez")
    void reintentoEspera() {
        when(ordenRepository.findById("ORD-1")).thenReturn(Optional.of(orden()));
        String clave = "k-esp";
        EsperaRequest pet = new EsperaRequest("VIA-1", "DESCARGA", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO);

        var res1 = servicio.reportarEspera("ORD-1", clave, pet);
        assertThat(res1.repetida()).isFalse();

        when(idempotencia.findById(clave)).thenReturn(Optional.of(new PeticionIdempotente(clave, "ORD-1", OffsetDateTime.now())));

        var res2 = servicio.reportarEspera("ORD-1", clave, pet);
        assertThat(res2.repetida()).isTrue();
        verify(idempotencia, times(1)).save(any(PeticionIdempotente.class));
    }
}
