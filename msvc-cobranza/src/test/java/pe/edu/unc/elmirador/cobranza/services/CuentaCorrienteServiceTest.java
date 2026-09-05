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

import pe.edu.unc.elmirador.cobranza.dto.request.RegistrarCuentaPorCobrarRequest;
import pe.edu.unc.elmirador.cobranza.dto.response.CuentaCorrienteResponse;
import pe.edu.unc.elmirador.cobranza.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.cobranza.exceptions.ImportesInconsistentesException;
import pe.edu.unc.elmirador.cobranza.exceptions.RehabilitacionInvalidaException;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaCorrienteDelCliente;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaPorCobrar;
import pe.edu.unc.elmirador.cobranza.models.vo.Dinero;
import pe.edu.unc.elmirador.cobranza.models.vo.EstadoCrediticio;
import pe.edu.unc.elmirador.cobranza.models.vo.SituacionCrediticia;
import pe.edu.unc.elmirador.cobranza.repositories.CuentaCorrienteDelClienteRepository;

class CuentaCorrienteServiceTest {

    private CuentaCorrienteDelClienteRepository repositorio;
    private CuentaCorrienteService servicio;
    private Clock relojFijo;

    @BeforeEach
    void setUp() {
        repositorio = mock(CuentaCorrienteDelClienteRepository.class);
        relojFijo = Clock.fixed(Instant.parse("2026-05-10T10:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new CuentaCorrienteService(repositorio, relojFijo);
    }

    @Test
    void registrarCuentaPorCobrar_inconsistente_lanzaExcepcionDominio() {
        // FAC-04 en la frontera: neto + detraccion != total
        RegistrarCuentaPorCobrarRequest req = new RegistrarCuentaPorCobrarRequest(
                "cli-1", "fac-1", "doc-1",
                new BigDecimal("100.00"), "PEN",
                new BigDecimal("10.00"), "PEN",
                new BigDecimal("80.00"), "PEN", // should be 90
                LocalDate.now(relojFijo)
        );

        when(repositorio.findByClienteId("cli-1")).thenReturn(Optional.empty());

        Throwable error = catchThrowable(() -> servicio.registrarCuentaPorCobrar(req));

        assertThat(error).isInstanceOf(ImportesInconsistentesException.class);
    }

    @Test
    void rehabilitar_lanzaRehabilitacionInvalida_siHayCuentasVencidasPorMasDe30Dias() {
        // CCC-01
        CuentaPorCobrar cuentaVencida = new CuentaPorCobrar(
                "cpc-1", "cli-1", "fac-1", "doc-1",
                new Dinero(new BigDecimal("100.00"), "PEN"),
                Dinero.cero("PEN"),
                new Dinero(new BigDecimal("100.00"), "PEN"),
                LocalDate.now(relojFijo).minusDays(45) // > 30 days
        );
        CuentaCorrienteDelCliente cliente = new CuentaCorrienteDelCliente("cli-1", EstadoCrediticio.suspendido("x", LocalDate.now(relojFijo)), new ArrayList<>(List.of(cuentaVencida)));
        when(repositorio.findByClienteId("cli-1")).thenReturn(Optional.of(cliente));

        Throwable error = catchThrowable(() -> servicio.rehabilitar("cli-1"));

        assertThat(error).isInstanceOf(RehabilitacionInvalidaException.class);
    }

    @Test
    void evaluarCredito_suspendeAutomaticamenteSiHayCuentaDeMasDe30Dias() {
        CuentaPorCobrar cuentaVencida = new CuentaPorCobrar(
                "cpc-1", "cli-1", "fac-1", "doc-1",
                new Dinero(new BigDecimal("100.00"), "PEN"),
                Dinero.cero("PEN"),
                new Dinero(new BigDecimal("100.00"), "PEN"),
                LocalDate.now(relojFijo).minusDays(31) // exceeds 30
        );
        CuentaCorrienteDelCliente cliente = new CuentaCorrienteDelCliente("cli-1", EstadoCrediticio.vigente(LocalDate.now(relojFijo)), new ArrayList<>(List.of(cuentaVencida)));
        when(repositorio.findByClienteId("cli-1")).thenReturn(Optional.of(cliente));
        when(repositorio.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CuentaCorrienteResponse res = servicio.evaluarCredito("cli-1");

        assertThat(res.situacion()).isEqualTo(SituacionCrediticia.SUSPENDIDO);
        verify(repositorio).save(cliente);
    }
}
