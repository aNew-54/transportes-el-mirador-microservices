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
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.edu.unc.elmirador.comercial.dto.request.AceptarCotizacionRequest;
import pe.edu.unc.elmirador.comercial.dto.response.CotizacionResponse;
import pe.edu.unc.elmirador.comercial.exceptions.CotizacionVencidaException;
import pe.edu.unc.elmirador.comercial.models.entity.Cliente;
import pe.edu.unc.elmirador.comercial.models.entity.Cotizacion;
import pe.edu.unc.elmirador.comercial.models.entity.OrdenDeServicio;
import pe.edu.unc.elmirador.comercial.models.vo.Carga;
import pe.edu.unc.elmirador.comercial.models.vo.CondicionDePago;
import pe.edu.unc.elmirador.comercial.models.vo.Dinero;
import pe.edu.unc.elmirador.comercial.models.vo.EstadoCrediticio;
import pe.edu.unc.elmirador.comercial.models.vo.EstadoDeCotizacion;
import pe.edu.unc.elmirador.comercial.models.vo.ModalidadDePago;
import pe.edu.unc.elmirador.comercial.models.vo.PeriodoDeVigencia;
import pe.edu.unc.elmirador.comercial.models.vo.RazonSocial;
import pe.edu.unc.elmirador.comercial.models.vo.Ruc;
import pe.edu.unc.elmirador.comercial.models.vo.Ruta;
import pe.edu.unc.elmirador.comercial.models.vo.SituacionCrediticia;
import pe.edu.unc.elmirador.comercial.models.vo.Tarifa;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeCarga;
import pe.edu.unc.elmirador.comercial.repositories.ClienteRepository;
import pe.edu.unc.elmirador.comercial.repositories.CotizacionRepository;
import pe.edu.unc.elmirador.comercial.repositories.OrdenDeServicioRepository;
import pe.edu.unc.elmirador.comercial.repositories.TarifarioRepository;

class CotizacionServiceTest {

    private CotizacionRepository cotizacionRepository;
    private ClienteRepository clienteRepository;
    private TarifarioRepository tarifarioRepository;
    private OrdenDeServicioRepository ordenRepository;
    private Clock reloj;
    private CotizacionService servicio;

    @BeforeEach
    void setUp() {
        cotizacionRepository = mock(CotizacionRepository.class);
        clienteRepository = mock(ClienteRepository.class);
        tarifarioRepository = mock(TarifarioRepository.class);
        ordenRepository = mock(OrdenDeServicioRepository.class);
        reloj = Clock.fixed(Instant.parse("2026-09-04T10:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new CotizacionService(cotizacionRepository, clienteRepository, tarifarioRepository, ordenRepository, reloj);
    }

    @Test
    void aceptar_cotizacionVigente_aceptaYGeneraOrden() {
        Cotizacion cotizacion = new Cotizacion(
                "cot-1", "cli-1", "tar-1",
                new Carga(1000, new BigDecimal("10.00"), TipoDeCarga.GENERAL),
                new Ruta("LIMA", "PIURA", "NORTE"),
                new Tarifa(new Dinero(new BigDecimal("1000.00"), "PEN"), List.of(), null),
                new PeriodoDeVigencia(LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-07")),
                EstadoDeCotizacion.EMITIDA,
                null
        );

        Cliente cliente = new Cliente(
                "cli-1", new Ruc("20123456789"), new RazonSocial("Acme S.A."),
                new CondicionDePago(ModalidadDePago.CREDITO, 30),
                new EstadoCrediticio(SituacionCrediticia.VIGENTE, LocalDate.parse("2026-09-04"))
        );

        when(cotizacionRepository.findById("cot-1")).thenReturn(Optional.of(cotizacion));
        when(clienteRepository.findById("cli-1")).thenReturn(Optional.of(cliente));
        when(cotizacionRepository.save(any())).thenReturn(cotizacion);

        AceptarCotizacionRequest peticion = new AceptarCotizacionRequest(ModalidadDePago.CREDITO, 30);
        CotizacionResponse respuesta = servicio.aceptar("cot-1", peticion);

        assertEquals("ACEPTADA", respuesta.estado());
        verify(ordenRepository).save(any(OrdenDeServicio.class));
    }

    @Test
    void aceptar_cotizacionVencida_lanzaCotizacionVencidaException() {
        Cotizacion cotizacion = new Cotizacion(
                "cot-1", "cli-1", "tar-1",
                new Carga(1000, new BigDecimal("10.00"), TipoDeCarga.GENERAL),
                new Ruta("LIMA", "PIURA", "NORTE"),
                new Tarifa(new Dinero(new BigDecimal("1000.00"), "PEN"), List.of(), null),
                new PeriodoDeVigencia(LocalDate.parse("2026-08-01"), LocalDate.parse("2026-08-07")),
                EstadoDeCotizacion.EMITIDA,
                null
        );

        when(cotizacionRepository.findById("cot-1")).thenReturn(Optional.of(cotizacion));

        AceptarCotizacionRequest peticion = new AceptarCotizacionRequest(ModalidadDePago.CREDITO, 30);
        assertThrows(CotizacionVencidaException.class, () -> servicio.aceptar("cot-1", peticion));
    }
}
