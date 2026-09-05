package pe.edu.unc.elmirador.cobranza.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pe.edu.unc.elmirador.cobranza.dto.response.CuentaCorrienteResponse;
import pe.edu.unc.elmirador.cobranza.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.cobranza.exceptions.RehabilitacionInvalidaException;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaCorrienteDelCliente;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaPorCobrar;
import pe.edu.unc.elmirador.cobranza.models.vo.Dinero;
import pe.edu.unc.elmirador.cobranza.models.vo.EstadoCrediticio;
import pe.edu.unc.elmirador.cobranza.repositories.CuentaCorrienteDelClienteRepository;

class CuentaCorrienteServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 5, 10);

    private CuentaCorrienteDelClienteRepository repositorio;
    private CuentaCorrienteService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(CuentaCorrienteDelClienteRepository.class);
        Clock relojFijo = Clock.fixed(
                Instant.parse("2026-05-10T15:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new CuentaCorrienteService(repositorio, relojFijo);
        when(repositorio.save(any(CuentaCorrienteDelCliente.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private CuentaPorCobrar cuenta(String id, String total, String moneda, LocalDate vencimiento) {
        return new CuentaPorCobrar(id, "cli-1", "fac-" + id, "doc-" + id,
                new Dinero(new BigDecimal(total), moneda), Dinero.cero(moneda),
                new Dinero(new BigDecimal(total), moneda), vencimiento);
    }

    /** Con detraccion del 12 %: registrar el deposito solo tiene sentido si la cuenta la tiene. */
    private CuentaPorCobrar cuentaConDetraccion(String id, String total, String detraccion, String neto) {
        return new CuentaPorCobrar(id, "cli-1", "fac-" + id, "doc-" + id,
                new Dinero(new BigDecimal(total), "PEN"), new Dinero(new BigDecimal(detraccion), "PEN"),
                new Dinero(new BigDecimal(neto), "PEN"), HOY.plusDays(10));
    }

    private CuentaCorrienteDelCliente clienteCon(EstadoCrediticio estado, CuentaPorCobrar... cuentas) {
        return new CuentaCorrienteDelCliente("cli-1", estado, new ArrayList<>(List.of(cuentas)));
    }

    @Test
    @DisplayName("un cliente sin cuenta corriente es 404")
    void clienteInexistente() {
        when(repositorio.findByClienteId("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.porClienteId("no-existe"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    /**
     * {@code deudaTotal(codigoMoneda)} exige la moneda para que nadie la adivine. La respuesta lleva
     * un total por cada moneda que el cliente debe de verdad, no un unico total en una moneda elegida
     * por la capa de presentacion.
     */
    @Test
    @DisplayName("la deuda sale desglosada por moneda, sin elegir ninguna por defecto")
    void deudaPorMoneda() {
        when(repositorio.findByClienteId("cli-1")).thenReturn(Optional.of(clienteCon(
                EstadoCrediticio.vigente(HOY),
                cuenta("cpc-1", "100.00", "PEN", HOY.plusDays(30)),
                cuenta("cpc-2", "250.00", "USD", HOY.plusDays(15)))));

        CuentaCorrienteResponse respuesta = servicio.porClienteId("cli-1");

        assertThat(respuesta.deudaPorMoneda())
                .extracting(i -> i.moneda() + " " + i.monto().stripTrailingZeros().toPlainString())
                .containsExactlyInAnyOrder("PEN 100", "USD 250");
    }

    @Test
    @DisplayName("un cliente sin cuentas debe cero en ninguna moneda, y no revienta")
    void clienteSinCuentas() {
        when(repositorio.findByClienteId("cli-1"))
                .thenReturn(Optional.of(clienteCon(EstadoCrediticio.vigente(HOY))));

        assertThat(servicio.porClienteId("cli-1").deudaPorMoneda()).isEmpty();
    }

    @Test
    @DisplayName("rehabilitar con una cuenta de mas de treinta dias de atraso deja subir CCC-01")
    void rehabilitarConDeudaVencida() {
        when(repositorio.findByClienteId("cli-1")).thenReturn(Optional.of(clienteCon(
                EstadoCrediticio.suspendido("mora", HOY),
                cuenta("cpc-1", "100.00", "PEN", HOY.minusDays(45)))));

        assertThatThrownBy(() -> servicio.rehabilitar("cli-1"))
                .isInstanceOf(RehabilitacionInvalidaException.class);
    }

    /**
     * La detraccion se registra buscando al titular por el id de la cuenta, no recorriendo todos los
     * clientes: la cuenta por cobrar es entidad hija y no tiene repositorio propio.
     */
    @Test
    @DisplayName("registrar la detraccion busca al titular por el id de la cuenta")
    void registrarDetraccion() {
        CuentaPorCobrar objetivo = cuentaConDetraccion("cpc-7", "100.00", "12.00", "88.00");
        CuentaCorrienteDelCliente titular = clienteCon(EstadoCrediticio.vigente(HOY), objetivo);
        when(repositorio.findByCuentasId("cpc-7")).thenReturn(Optional.of(titular));

        var respuesta = servicio.registrarDetraccion("cpc-7");

        assertThat(respuesta.detraccionDepositada()).isTrue();
        verify(repositorio).save(titular);
    }

    @Test
    @DisplayName("registrar la detraccion de una cuenta que no existe es 404")
    void registrarDetraccionInexistente() {
        when(repositorio.findByCuentasId("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.registrarDetraccion("no-existe"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
