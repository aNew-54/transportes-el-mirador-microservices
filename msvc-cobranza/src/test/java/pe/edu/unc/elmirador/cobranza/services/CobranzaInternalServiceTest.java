package pe.edu.unc.elmirador.cobranza.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import pe.edu.unc.elmirador.cobranza.dto.internal.request.CondicionDePagoRequest;
import pe.edu.unc.elmirador.cobranza.dto.internal.request.CrearCuentaPorCobrarRequest;
import pe.edu.unc.elmirador.cobranza.dto.internal.request.DetraccionRequest;
import pe.edu.unc.elmirador.cobranza.dto.internal.request.ImporteRequest;
import pe.edu.unc.elmirador.cobranza.dto.internal.response.CuentaPorCobrarCreadaResponse;
import pe.edu.unc.elmirador.cobranza.dto.internal.response.EstadoCrediticioResponse;
import pe.edu.unc.elmirador.cobranza.dto.response.ResultadoIdempotente;
import pe.edu.unc.elmirador.cobranza.exceptions.ImportesInconsistentesException;
import pe.edu.unc.elmirador.cobranza.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaCorrienteDelCliente;
import pe.edu.unc.elmirador.cobranza.models.entity.PeticionIdempotente;
import pe.edu.unc.elmirador.cobranza.models.vo.EstadoCrediticio;
import pe.edu.unc.elmirador.cobranza.repositories.CuentaCorrienteDelClienteRepository;
import pe.edu.unc.elmirador.cobranza.repositories.PeticionIdempotenteRepository;

class CobranzaInternalServiceTest {

    private CuentaCorrienteDelClienteRepository repositorio;
    private PeticionIdempotenteRepository idempotencia;
    private CobranzaInternalService servicio;
    private Clock reloj;
    private LocalDate hoy;

    @BeforeEach
    void setup() {
        repositorio = Mockito.mock(CuentaCorrienteDelClienteRepository.class);
        idempotencia = Mockito.mock(PeticionIdempotenteRepository.class);
        hoy = LocalDate.of(2026, 9, 10);
        reloj = Clock.fixed(Instant.parse("2026-09-10T16:00:00Z"), ZoneId.of("America/Lima"));
        servicio = new CobranzaInternalService(repositorio, idempotencia, reloj);
    }

    @Test
    void consultarEstadoCrediticio_DebeDevolverDeudaPorMoneda() {
        CuentaCorrienteDelCliente cuenta = new CuentaCorrienteDelCliente("CLI-007", EstadoCrediticio.vigente(hoy));
        when(repositorio.findByClienteId("CLI-007")).thenReturn(Optional.of(cuenta));

        EstadoCrediticioResponse resp = servicio.estadoCrediticio("CLI-007");

        assertThat(resp.clienteId()).isEqualTo("CLI-007");
        assertThat(resp.situacion()).isEqualTo("VIGENTE");
        assertThat(resp.deudaPorMoneda()).isEmpty();
    }

    @Test
    void consultarEstadoCrediticio_NoExiste_LanzaException() {
        when(repositorio.findByClienteId("CLI-007")).thenReturn(Optional.empty());

        Throwable error = catchThrowable(() -> servicio.estadoCrediticio("CLI-007"));

        assertThat(error).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void crearCuenta_Idempotencia_MismaClave_NoRepiteEfecto() {
        String clave = "FAC-2026-000310";
        PeticionIdempotente huella = new PeticionIdempotente(clave, "cuenta-id", OffsetDateTime.now(reloj));
        when(idempotencia.findById(clave)).thenReturn(Optional.of(huella));

        CrearCuentaPorCobrarRequest peticion = new CrearCuentaPorCobrarRequest(
                "FAC-2026-000310",
                "F001-00000310",
                "CLI-0007",
                new ImporteRequest("1821.60", "PEN"),
                new DetraccionRequest(BigDecimal.valueOf(4), "72.86", "PEN", "00-123-456789"),
                new ImporteRequest("1748.74", "PEN"),
                OffsetDateTime.now(reloj),
                OffsetDateTime.now(reloj),
                new CondicionDePagoRequest("CREDITO", 30)
        );

        ResultadoIdempotente<CuentaPorCobrarCreadaResponse> resultado = servicio.crearCuentaPorCobrar(clave, peticion);

        assertThat(resultado.repetida()).isTrue();
        assertThat(resultado.cuerpo().cuentaId()).isEqualTo("cuenta-id");
        verify(repositorio, never()).save(any());
    }

    @Test
    void crearCuenta_FallaPorFAC04() {
        String clave = "FAC-2026-000310";
        when(idempotencia.findById(clave)).thenReturn(Optional.empty());

        CrearCuentaPorCobrarRequest peticion = new CrearCuentaPorCobrarRequest(
                "FAC-2026-000310",
                "F001-00000310",
                "CLI-0007",
                new ImporteRequest("1821.60", "PEN"),
                new DetraccionRequest(BigDecimal.valueOf(4), "72.86", "PEN", "00-123-456789"),
                new ImporteRequest("1700.00", "PEN"), // Incorrecto
                OffsetDateTime.now(reloj),
                OffsetDateTime.now(reloj),
                new CondicionDePagoRequest("CREDITO", 30)
        );

        Throwable error = catchThrowable(() -> servicio.crearCuentaPorCobrar(clave, peticion));

        assertThat(error).isInstanceOf(ImportesInconsistentesException.class);
        verify(repositorio, never()).save(any());
    }

    @Test
    void crearCuenta_Contado_Ignora() {
        String clave = "FAC-2026-000310";
        when(idempotencia.findById(clave)).thenReturn(Optional.empty());

        CrearCuentaPorCobrarRequest peticion = new CrearCuentaPorCobrarRequest(
                "FAC-2026-000310",
                "F001-00000310",
                "CLI-0007",
                new ImporteRequest("1821.60", "PEN"),
                new DetraccionRequest(BigDecimal.valueOf(4), "72.86", "PEN", "00-123-456789"),
                new ImporteRequest("1748.74", "PEN"),
                OffsetDateTime.now(reloj),
                OffsetDateTime.now(reloj),
                new CondicionDePagoRequest("CONTADO", 0)
        );

        ResultadoIdempotente<CuentaPorCobrarCreadaResponse> resultado = servicio.crearCuentaPorCobrar(clave, peticion);

        assertThat(resultado.repetida()).isFalse();
        assertThat(resultado.cuerpo().cuentaId()).startsWith("CONTADO-");
        verify(idempotencia, times(1)).save(any());
        verify(repositorio, never()).save(any());
    }

    @Test
    void crearCuenta_Exito() {
        String clave = "FAC-2026-000310";
        when(idempotencia.findById(clave)).thenReturn(Optional.empty());
        
        CuentaCorrienteDelCliente cuenta = new CuentaCorrienteDelCliente("CLI-0007", EstadoCrediticio.vigente(hoy));
        when(repositorio.findByClienteId("CLI-0007")).thenReturn(Optional.of(cuenta));

        CrearCuentaPorCobrarRequest peticion = new CrearCuentaPorCobrarRequest(
                "FAC-2026-000310",
                "F001-00000310",
                "CLI-0007",
                new ImporteRequest("1821.60", "PEN"),
                new DetraccionRequest(BigDecimal.valueOf(4), "72.86", "PEN", "00-123-456789"),
                new ImporteRequest("1748.74", "PEN"),
                OffsetDateTime.now(reloj),
                OffsetDateTime.now(reloj),
                new CondicionDePagoRequest("CREDITO", 30)
        );

        ResultadoIdempotente<CuentaPorCobrarCreadaResponse> resultado = servicio.crearCuentaPorCobrar(clave, peticion);

        assertThat(resultado.repetida()).isFalse();
        assertThat(resultado.cuerpo().cuentaId()).isNotBlank();
        
        verify(repositorio, times(1)).save(any());
        verify(idempotencia, times(1)).save(any());
    }
}
