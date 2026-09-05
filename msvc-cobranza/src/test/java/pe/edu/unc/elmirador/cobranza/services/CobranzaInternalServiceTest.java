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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import pe.edu.unc.elmirador.cobranza.models.vo.CondicionDeVenta;
import pe.edu.unc.elmirador.cobranza.dto.internal.request.CondicionDePagoRequest;
import pe.edu.unc.elmirador.cobranza.dto.internal.request.CrearCuentaPorCobrarRequest;
import pe.edu.unc.elmirador.cobranza.dto.internal.request.DetraccionRequest;
import pe.edu.unc.elmirador.cobranza.dto.internal.response.ImporteResponse;
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
    @DisplayName("[CCC] Un cliente del que Cobranza no sabe nada tiene el credito intacto")
    void consultarEstadoCrediticio_ClienteNuevo_AbreLaCuentaYResponde() {
        when(repositorio.findByClienteId("CLI-NUEVO")).thenReturn(Optional.empty());
        when(repositorio.save(any(CuentaCorrienteDelCliente.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        EstadoCrediticioResponse resp = servicio.estadoCrediticio("CLI-NUEVO");

        // Antes esto lanzaba RecursoNoEncontradoException, y como ningun camino de produccion
        // construia nunca una CuentaCorrienteDelCliente, el 404 era la unica respuesta posible del
        // contrato 11: ningun cliente podia pedir una orden a credito.
        assertThat(resp.situacion()).isEqualTo("VIGENTE");
        assertThat(resp.deudaPorMoneda()).isEmpty();
        assertThat(resp.cuentasVencidas()).isZero();
        assertThat(resp.fechaDeCambio()).isEqualTo(hoy);
        verify(repositorio).save(any(CuentaCorrienteDelCliente.class));
    }

    @Test
    @DisplayName("[CCC] La cuenta se persiste: fechaDeCambio no puede moverse en cada lectura")
    void consultarEstadoCrediticio_ClienteNuevo_PersisteLaCuenta() {
        when(repositorio.findByClienteId("CLI-NUEVO")).thenReturn(Optional.empty());
        when(repositorio.save(any(CuentaCorrienteDelCliente.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        servicio.estadoCrediticio("CLI-NUEVO");

        ArgumentCaptor<CuentaCorrienteDelCliente> captor =
                ArgumentCaptor.forClass(CuentaCorrienteDelCliente.class);
        verify(repositorio).save(captor.capture());
        // Comercial guarda esta fecha y la compara por dia. Sintetizarla en cada lectura daria hoy
        // siempre, que es mentir sobre cuando cambio la situacion del cliente.
        assertThat(captor.getValue().clienteId()).isEqualTo("CLI-NUEVO");
        assertThat(captor.getValue().estado().fechaDeCambio()).isEqualTo(hoy);
        assertThat(captor.getValue().estado().permiteCredito()).isTrue();
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
                new CondicionDePagoRequest(CondicionDeVenta.CREDITO, 30)
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
                new CondicionDePagoRequest(CondicionDeVenta.CREDITO, 30)
        );

        Throwable error = catchThrowable(() -> servicio.crearCuentaPorCobrar(clave, peticion));

        assertThat(error).isInstanceOf(ImportesInconsistentesException.class);
        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("[CCC] La primera factura de un cliente nuevo entra al ledger, no responde 404")
    void crearCuenta_ClienteSinCuentaCorriente_AbreLaCuenta() {
        String clave = "FAC-2026-000999";
        when(idempotencia.findById(clave)).thenReturn(Optional.empty());
        when(repositorio.findByClienteId("CLI-NUEVO")).thenReturn(Optional.empty());
        when(repositorio.save(any(CuentaCorrienteDelCliente.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        CrearCuentaPorCobrarRequest peticion = new CrearCuentaPorCobrarRequest(
                "FAC-2026-000999",
                "F001-00000999",
                "CLI-NUEVO",
                new ImporteRequest("1821.60", "PEN"),
                new DetraccionRequest(BigDecimal.valueOf(4), "72.86", "PEN", "00-123-456789"),
                new ImporteRequest("1748.74", "PEN"),
                OffsetDateTime.now(reloj),
                OffsetDateTime.now(reloj),
                new CondicionDePagoRequest(CondicionDeVenta.CREDITO, 30)
        );

        // Antes esto lanzaba RecursoNoEncontradoException. Como nada creaba nunca una cuenta
        // corriente, la primera factura de cualquier cliente era tambien la ultima: el contrato 10
        // respondia 404 y Facturacion lo leia como un 503.
        ResultadoIdempotente<CuentaPorCobrarCreadaResponse> resultado =
                servicio.crearCuentaPorCobrar(clave, peticion);

        assertThat(resultado.repetida()).isFalse();
        assertThat(resultado.cuerpo().facturaId()).isEqualTo("FAC-2026-000999");
        verify(repositorio, times(2)).save(any(CuentaCorrienteDelCliente.class));
    }

    @Test
    @DisplayName("[contrato 10] Una factura sin detraccion se registra: no hay cuenta que exigir")
    void crearCuenta_SinDetraccion_NoExigeCuentaBancaria() {
        String clave = "FAC-2026-000777";
        when(idempotencia.findById(clave)).thenReturn(Optional.empty());
        when(repositorio.findByClienteId("CLI-0007")).thenReturn(Optional.of(
                new CuentaCorrienteDelCliente("CLI-0007", EstadoCrediticio.vigente(hoy))));

        // La detraccion solo aplica por encima de un umbral; por debajo no hay monto ni cuenta donde
        // depositarlo. cuentaBancaria llevaba @NotBlank, asi que estas facturas —que Facturacion
        // emite sin problema— salian con un 400 y no podian entrar nunca a la cartera.
        CrearCuentaPorCobrarRequest peticion = new CrearCuentaPorCobrarRequest(
                "FAC-2026-000777",
                "F001-00000777",
                "CLI-0007",
                new ImporteRequest("1500.00", "PEN"),
                new DetraccionRequest(BigDecimal.ZERO, "0.00", "PEN", ""),
                new ImporteRequest("1500.00", "PEN"),
                OffsetDateTime.now(reloj),
                OffsetDateTime.now(reloj),
                new CondicionDePagoRequest(CondicionDeVenta.CREDITO, 30)
        );

        ResultadoIdempotente<CuentaPorCobrarCreadaResponse> resultado =
                servicio.crearCuentaPorCobrar(clave, peticion);

        assertThat(resultado.repetida()).isFalse();
    }

    @Test
    @DisplayName("[contrato 10] Si se detrajo dinero, hay que decir a que cuenta fue")
    void crearCuenta_ConDetraccionSinCuenta_Rechaza() {
        String clave = "FAC-2026-000778";
        when(idempotencia.findById(clave)).thenReturn(Optional.empty());

        CrearCuentaPorCobrarRequest peticion = new CrearCuentaPorCobrarRequest(
                "FAC-2026-000778",
                "F001-00000778",
                "CLI-0007",
                new ImporteRequest("1821.60", "PEN"),
                new DetraccionRequest(BigDecimal.valueOf(4), "72.86", "PEN", "  "),
                new ImporteRequest("1748.74", "PEN"),
                OffsetDateTime.now(reloj),
                OffsetDateTime.now(reloj),
                new CondicionDePagoRequest(CondicionDeVenta.CREDITO, 30)
        );

        assertThat(catchThrowable(() -> servicio.crearCuentaPorCobrar(clave, peticion)))
                .isInstanceOf(ImportesInconsistentesException.class)
                .hasMessageContaining("cuenta bancaria");
        verify(repositorio, never()).save(any());
    }

    @Test
    void crearCuenta_Contado_NaceCancelada() {
        String clave = "FAC-2026-000310";
        when(idempotencia.findById(clave)).thenReturn(Optional.empty());

        CuentaCorrienteDelCliente cuentaCorriente =
                new CuentaCorrienteDelCliente("CLI-0007", EstadoCrediticio.vigente(hoy));
        when(repositorio.findByClienteId("CLI-0007")).thenReturn(Optional.of(cuentaCorriente));

        CrearCuentaPorCobrarRequest peticion = new CrearCuentaPorCobrarRequest(
                "FAC-2026-000310",
                "F001-00000310",
                "CLI-0007",
                new ImporteRequest("1821.60", "PEN"),
                new DetraccionRequest(BigDecimal.valueOf(4), "72.86", "PEN", "00-123-456789"),
                new ImporteRequest("1748.74", "PEN"),
                OffsetDateTime.now(reloj),
                OffsetDateTime.now(reloj),
                new CondicionDePagoRequest(CondicionDeVenta.CONTADO, 0)
        );

        ResultadoIdempotente<CuentaPorCobrarCreadaResponse> resultado = servicio.crearCuentaPorCobrar(clave, peticion);

        // El contrato dice que la factura al contado «se registra ya cancelada». Se registra: hay un
        // recurso de verdad detras del 201, con su identificador y su historia. La version anterior
        // devolvia "CONTADO-" + UUID sin crear nada, de modo que el 201 apuntaba a la nada.
        assertThat(resultado.repetida()).isFalse();
        assertThat(resultado.cuerpo().cuentaId()).doesNotStartWith("CONTADO-");
        assertThat(cuentaCorriente.cuentas()).hasSize(1);
        assertThat(cuentaCorriente.cuentas().getFirst().estaCancelada()).isTrue();
        assertThat(cuentaCorriente.cuentas().getFirst().saldo().esCero()).isTrue();
        verify(repositorio, times(1)).save(cuentaCorriente);
        verify(idempotencia, times(1)).save(any());
    }

    @Test
    void crearCuenta_ACredito_NaceViva() {
        String clave = "FAC-2026-000311";
        when(idempotencia.findById(clave)).thenReturn(Optional.empty());

        CuentaCorrienteDelCliente cuentaCorriente =
                new CuentaCorrienteDelCliente("CLI-0007", EstadoCrediticio.vigente(hoy));
        when(repositorio.findByClienteId("CLI-0007")).thenReturn(Optional.of(cuentaCorriente));

        CrearCuentaPorCobrarRequest peticion = new CrearCuentaPorCobrarRequest(
                "FAC-2026-000311",
                "F001-00000311",
                "CLI-0007",
                new ImporteRequest("1821.60", "PEN"),
                new DetraccionRequest(BigDecimal.valueOf(4), "72.86", "PEN", "00-123-456789"),
                new ImporteRequest("1748.74", "PEN"),
                OffsetDateTime.now(reloj),
                OffsetDateTime.now(reloj),
                new CondicionDePagoRequest(CondicionDeVenta.CREDITO, 30)
        );

        servicio.crearCuentaPorCobrar(clave, peticion);

        assertThat(cuentaCorriente.cuentas().getFirst().estaCancelada()).isFalse();
        assertThat(cuentaCorriente.cuentas().getFirst().saldo().monto()).isEqualByComparingTo("1748.74");
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
                new CondicionDePagoRequest(CondicionDeVenta.CREDITO, 30)
        );

        ResultadoIdempotente<CuentaPorCobrarCreadaResponse> resultado = servicio.crearCuentaPorCobrar(clave, peticion);

        assertThat(resultado.repetida()).isFalse();
        assertThat(resultado.cuerpo().cuentaId()).isNotBlank();
        
        verify(repositorio, times(1)).save(any());
        verify(idempotencia, times(1)).save(any());
    }
}
