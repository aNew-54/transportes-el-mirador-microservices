package pe.edu.unc.elmirador.cobranza.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pe.edu.unc.elmirador.cobranza.dto.request.AplicacionRequest;
import pe.edu.unc.elmirador.cobranza.dto.request.AplicarPagoRequest;
import pe.edu.unc.elmirador.cobranza.dto.request.RegistrarPagoRequest;
import pe.edu.unc.elmirador.cobranza.dto.response.PagoResponse;
import pe.edu.unc.elmirador.cobranza.exceptions.AplicacionExcedeElPagoException;
import pe.edu.unc.elmirador.cobranza.exceptions.PagoDeOtroClienteException;
import pe.edu.unc.elmirador.cobranza.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaCorrienteDelCliente;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaPorCobrar;
import pe.edu.unc.elmirador.cobranza.models.entity.Pago;
import pe.edu.unc.elmirador.cobranza.models.vo.Dinero;
import pe.edu.unc.elmirador.cobranza.models.vo.EstadoCrediticio;
import pe.edu.unc.elmirador.cobranza.models.vo.MedioDePago;
import pe.edu.unc.elmirador.cobranza.models.vo.ModalidadDePago;
import pe.edu.unc.elmirador.cobranza.repositories.CuentaCorrienteDelClienteRepository;
import pe.edu.unc.elmirador.cobranza.repositories.PagoRepository;

class PagoServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 5, 10);

    private PagoRepository pagoRepositorio;
    private CuentaCorrienteDelClienteRepository cuentaRepositorio;
    private PagoService servicio;

    @BeforeEach
    void preparar() {
        pagoRepositorio = mock(PagoRepository.class);
        cuentaRepositorio = mock(CuentaCorrienteDelClienteRepository.class);
        servicio = new PagoService(pagoRepositorio, cuentaRepositorio);
        when(pagoRepositorio.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Pago pagoDe(String clienteId, String monto) {
        return new Pago("p-1", clienteId, new Dinero(new BigDecimal(monto), "PEN"),
                new MedioDePago(ModalidadDePago.EFECTIVO, null), HOY);
    }

    private CuentaCorrienteDelCliente titularCon(String clienteId, CuentaPorCobrar cuenta) {
        return new CuentaCorrienteDelCliente(clienteId, EstadoCrediticio.vigente(HOY),
                new ArrayList<>(List.of(cuenta)));
    }

    private CuentaPorCobrar cuentaDe(String id, String clienteId, String total) {
        return new CuentaPorCobrar(id, clienteId, "fac-1", "doc-1",
                new Dinero(new BigDecimal(total), "PEN"), Dinero.cero("PEN"),
                new Dinero(new BigDecimal(total), "PEN"), HOY);
    }

    @Test
    @DisplayName("la fecha del pago llega en la peticion; el servicio no la inventa con el reloj")
    void laFechaVieneDelHecho() {
        PagoResponse respuesta = servicio.registrar(new RegistrarPagoRequest(
                "cli-1", new BigDecimal("100.00"), "PEN", ModalidadDePago.TRANSFERENCIA,
                "OP-778812", LocalDate.of(2026, 4, 30)));

        assertThat(respuesta.clienteId()).isEqualTo("cli-1");
        assertThat(respuesta.fecha()).isEqualTo(LocalDate.of(2026, 4, 30));
        verify(pagoRepositorio).save(any(Pago.class));
    }

    /**
     * PAG-02 es alcanzable porque la cuenta se busca por su propio id. Si se buscara dentro del
     * titular del pago, siempre seria suya y la invariante no podria violarse ni, por tanto, probarse.
     */
    @Test
    @DisplayName("aplicar a la cuenta de otro cliente deja subir PAG-02 sin transformarla")
    void aplicarACuentaAjena() {
        CuentaPorCobrar ajena = cuentaDe("cpc-9", "cli-2", "50.00");
        when(pagoRepositorio.findById("p-1")).thenReturn(Optional.of(pagoDe("cli-1", "100.00")));
        when(cuentaRepositorio.findByCuentasId("cpc-9"))
                .thenReturn(Optional.of(titularCon("cli-2", ajena)));

        assertThatThrownBy(() -> servicio.aplicar("p-1", new AplicarPagoRequest(
                List.of(new AplicacionRequest("cpc-9", new BigDecimal("10.00"), "PEN")))))
                .isInstanceOf(PagoDeOtroClienteException.class);
    }

    @Test
    @DisplayName("aplicar mas de lo que vale el pago deja subir PAG-01 sin transformarla")
    void aplicarMasDeLoQueValeElPago() {
        CuentaPorCobrar propia = cuentaDe("cpc-1", "cli-1", "50.00");
        when(pagoRepositorio.findById("p-1")).thenReturn(Optional.of(pagoDe("cli-1", "10.00")));
        when(cuentaRepositorio.findByCuentasId("cpc-1"))
                .thenReturn(Optional.of(titularCon("cli-1", propia)));

        assertThatThrownBy(() -> servicio.aplicar("p-1", new AplicarPagoRequest(
                List.of(new AplicacionRequest("cpc-1", new BigDecimal("20.00"), "PEN")))))
                .isInstanceOf(AplicacionExcedeElPagoException.class);
    }

    @Test
    @DisplayName("una cuenta que no existe es 404, no una invariante rota")
    void cuentaInexistente() {
        when(pagoRepositorio.findById("p-1")).thenReturn(Optional.of(pagoDe("cli-1", "100.00")));
        when(cuentaRepositorio.findByCuentasId("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.aplicar("p-1", new AplicarPagoRequest(
                List.of(new AplicacionRequest("no-existe", new BigDecimal("10.00"), "PEN")))))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("una aplicacion valida guarda el pago y al titular de la cuenta")
    void aplicacionValida() {
        CuentaPorCobrar propia = cuentaDe("cpc-1", "cli-1", "50.00");
        CuentaCorrienteDelCliente titular = titularCon("cli-1", propia);
        when(pagoRepositorio.findById("p-1")).thenReturn(Optional.of(pagoDe("cli-1", "100.00")));
        when(cuentaRepositorio.findByCuentasId("cpc-1")).thenReturn(Optional.of(titular));

        PagoResponse respuesta = servicio.aplicar("p-1", new AplicarPagoRequest(
                List.of(new AplicacionRequest("cpc-1", new BigDecimal("30.00"), "PEN"))));

        assertThat(respuesta.aplicaciones()).hasSize(1);
        assertThat(propia.aplicado().monto()).isEqualByComparingTo(new BigDecimal("30.00"));
        verify(cuentaRepositorio).save(titular);
    }

    @Test
    @DisplayName("el pago no existe: 404")
    void pagoInexistente() {
        when(pagoRepositorio.findById("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.aplicar("no-existe", new AplicarPagoRequest(
                List.of(new AplicacionRequest("cpc-1", new BigDecimal("10.00"), "PEN")))))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
