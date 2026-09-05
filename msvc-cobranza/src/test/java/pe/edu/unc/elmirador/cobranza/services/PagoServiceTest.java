package pe.edu.unc.elmirador.cobranza.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.edu.unc.elmirador.cobranza.dto.request.AplicacionRequest;
import pe.edu.unc.elmirador.cobranza.dto.request.AplicarPagoRequest;
import pe.edu.unc.elmirador.cobranza.dto.request.RegistrarPagoRequest;
import pe.edu.unc.elmirador.cobranza.dto.response.PagoResponse;
import pe.edu.unc.elmirador.cobranza.exceptions.AplicacionExcedeElPagoException;
import pe.edu.unc.elmirador.cobranza.exceptions.PagoDeOtroClienteException;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaCorrienteDelCliente;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaPorCobrar;
import pe.edu.unc.elmirador.cobranza.models.vo.Dinero;
import pe.edu.unc.elmirador.cobranza.models.vo.EstadoCrediticio;
import pe.edu.unc.elmirador.cobranza.models.vo.MedioDePago;
import pe.edu.unc.elmirador.cobranza.models.vo.ModalidadDePago;
import pe.edu.unc.elmirador.cobranza.repositories.CuentaCorrienteDelClienteRepository;
import pe.edu.unc.elmirador.cobranza.repositories.PagoRepository;

class PagoServiceTest {

    private PagoRepository pagoRepositorio;
    private CuentaCorrienteDelClienteRepository cuentaRepositorio;
    private PagoService servicio;
    private Clock relojFijo;

    @BeforeEach
    void setUp() {
        pagoRepositorio = mock(PagoRepository.class);
        cuentaRepositorio = mock(CuentaCorrienteDelClienteRepository.class);
        relojFijo = Clock.fixed(Instant.parse("2026-05-10T10:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new PagoService(pagoRepositorio, cuentaRepositorio, relojFijo);
    }

    @Test
    void registrarPago_guardaElPagoYDevuelveRespuesta() {
        RegistrarPagoRequest request = new RegistrarPagoRequest(
                "cli-1", new BigDecimal("100.00"), "PEN", ModalidadDePago.EFECTIVO, null);
        
        when(pagoRepositorio.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PagoResponse response = servicio.registrar(request);

        assertThat(response.clienteId()).isEqualTo("cli-1");
        assertThat(response.montoMonto()).isEqualByComparingTo(new BigDecimal("100.00"));
        verify(pagoRepositorio).save(any());
    }

    @Test
    void aplicarPago_fallaSiEsDeOtroCliente_sinTransformarExcepcion() {
        // PAG-02
        pe.edu.unc.elmirador.cobranza.models.entity.Pago pago = new pe.edu.unc.elmirador.cobranza.models.entity.Pago(
                "p-1", "cli-1", Dinero.cero("PEN").sumar(new Dinero(new BigDecimal("100.00"), "PEN")), 
                new MedioDePago(ModalidadDePago.EFECTIVO, null), LocalDate.now(relojFijo)
        );
        
        CuentaPorCobrar cuenta = new CuentaPorCobrar(
                "cpc-1", "cli-2", "fac-1", "doc-1", 
                new Dinero(new BigDecimal("50.00"), "PEN"), 
                Dinero.cero("PEN"), 
                new Dinero(new BigDecimal("50.00"), "PEN"), 
                LocalDate.now(relojFijo)
        );
        
        CuentaCorrienteDelCliente cliente2 = new CuentaCorrienteDelCliente("cli-2", EstadoCrediticio.vigente(LocalDate.now(relojFijo)), List.of(cuenta));

        when(pagoRepositorio.findById("p-1")).thenReturn(Optional.of(pago));
        when(cuentaRepositorio.findByClienteId("cli-1")).thenReturn(Optional.of(cliente2));

        AplicarPagoRequest req = new AplicarPagoRequest(List.of(
                new AplicacionRequest("cpc-1", new BigDecimal("10.00"), "PEN")
        ));

        Throwable error = catchThrowable(() -> servicio.aplicarPago("p-1", req));

        assertThat(error).isInstanceOf(PagoDeOtroClienteException.class);
    }
    
    @Test
    void aplicarPago_fallaSiExcedeMonto_sinTransformarExcepcion() {
        // PAG-01
        pe.edu.unc.elmirador.cobranza.models.entity.Pago pago = new pe.edu.unc.elmirador.cobranza.models.entity.Pago(
                "p-1", "cli-1", new Dinero(new BigDecimal("10.00"), "PEN"), 
                new MedioDePago(ModalidadDePago.EFECTIVO, null), LocalDate.now(relojFijo)
        );
        
        CuentaPorCobrar cuenta = new CuentaPorCobrar(
                "cpc-1", "cli-1", "fac-1", "doc-1", 
                new Dinero(new BigDecimal("50.00"), "PEN"), 
                Dinero.cero("PEN"), 
                new Dinero(new BigDecimal("50.00"), "PEN"), 
                LocalDate.now(relojFijo)
        );
        
        CuentaCorrienteDelCliente cliente1 = new CuentaCorrienteDelCliente("cli-1", EstadoCrediticio.vigente(LocalDate.now(relojFijo)), new ArrayList<>(List.of(cuenta)));

        when(pagoRepositorio.findById("p-1")).thenReturn(Optional.of(pago));
        when(cuentaRepositorio.findByClienteId("cli-1")).thenReturn(Optional.of(cliente1));

        AplicarPagoRequest req = new AplicarPagoRequest(List.of(
                new AplicacionRequest("cpc-1", new BigDecimal("20.00"), "PEN")
        ));

        Throwable error = catchThrowable(() -> servicio.aplicarPago("p-1", req));

        assertThat(error).isInstanceOf(AplicacionExcedeElPagoException.class);
    }
}
