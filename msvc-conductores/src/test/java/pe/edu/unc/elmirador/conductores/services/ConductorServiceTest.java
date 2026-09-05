package pe.edu.unc.elmirador.conductores.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import pe.edu.unc.elmirador.conductores.dto.request.RegistrarConductorRequest;
import pe.edu.unc.elmirador.conductores.dto.request.RegistrarInduccionRequest;
import pe.edu.unc.elmirador.conductores.dto.request.SuspenderConductorRequest;
import pe.edu.unc.elmirador.conductores.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.conductores.exceptions.NumeroDeLicenciaInvalidoException;
import pe.edu.unc.elmirador.conductores.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.conductores.exceptions.RehabilitacionInvalidaException;
import pe.edu.unc.elmirador.conductores.models.entity.Conductor;
import pe.edu.unc.elmirador.conductores.models.entity.Induccion;
import pe.edu.unc.elmirador.conductores.models.vo.CategoriaDeLicencia;
import pe.edu.unc.elmirador.conductores.models.vo.EstadoDeHabilitacion;
import pe.edu.unc.elmirador.conductores.models.vo.HorasDeConduccion;
import pe.edu.unc.elmirador.conductores.models.vo.NumeroDeLicencia;
import pe.edu.unc.elmirador.conductores.models.vo.PeriodoDeVigencia;
import pe.edu.unc.elmirador.conductores.models.vo.SituacionDeHabilitacion;
import pe.edu.unc.elmirador.conductores.repositories.ConductorRepository;

/**
 * El servicio de aplicacion orquesta y transacciona; no decide reglas. Estas pruebas comprueban las
 * dos cosas: que llama al agregado, y que cuando el agregado se niega la excepcion sale sin
 * transformar, para que la traduccion a HTTP quede en un solo sitio.
 */
class ConductorServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 3, 10);

    private ConductorRepository repositorio;
    private ConductorService servicio;

    @BeforeEach
    void preparar() {
        repositorio = Mockito.mock(ConductorRepository.class);
        Clock relojFijo = Clock.fixed(
                HOY.atStartOfDay(ZoneId.of("America/Lima")).toInstant(),
                ZoneId.of("America/Lima"));
        servicio = new ConductorService(repositorio, relojFijo);
        when(repositorio.save(any(Conductor.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Conductor conductorVigente() {
        return new Conductor(
                "c-1",
                "Juan Perez Vasquez",
                new NumeroDeLicencia("Q12345678"),
                CategoriaDeLicencia.A_IIIB,
                new PeriodoDeVigencia(LocalDate.of(2025, 1, 1), LocalDate.of(2027, 1, 1)),
                HorasDeConduccion.ventanaDe(HOY),
                EstadoDeHabilitacion.habilitado(),
                List.of());
    }

    @Test
    @DisplayName("registrar abre la ventana de computo con la fecha del reloj inyectado, no con la del sistema")
    void registrarUsaElRelojInyectado() {
        when(repositorio.findByNumeroDeLicenciaValor("Q12345678")).thenReturn(Optional.empty());

        servicio.registrar(new RegistrarConductorRequest(
                "Juan Perez Vasquez", "q12345678", CategoriaDeLicencia.A_IIIB,
                LocalDate.of(2025, 1, 1), LocalDate.of(2027, 1, 1)));

        ArgumentCaptor<Conductor> capturado = ArgumentCaptor.forClass(Conductor.class);
        verify(repositorio).save(capturado.capture());
        assertThat(capturado.getValue().getHorasAcumuladas().ventanaDeComputo().desde()).isEqualTo(HOY);
        assertThat(capturado.getValue().getNumeroDeLicencia().valor()).isEqualTo("Q12345678");
        assertThat(capturado.getValue().getEstado().estaHabilitado()).isTrue();
    }

    @Test
    @DisplayName("registrar con una licencia ya usada es un conflicto de recurso, no una invariante rota")
    void registrarConLicenciaDuplicada() {
        when(repositorio.findByNumeroDeLicenciaValor("Q12345678"))
                .thenReturn(Optional.of(conductorVigente()));

        assertThatThrownBy(() -> servicio.registrar(new RegistrarConductorRequest(
                "Otro Conductor", "Q12345678", CategoriaDeLicencia.A_IIIA,
                LocalDate.of(2025, 1, 1), LocalDate.of(2027, 1, 1))))
                .isInstanceOf(ConflictoDeRecursoException.class);

        verify(repositorio, never()).save(any(Conductor.class));
    }

    @Test
    @DisplayName("el formato del numero de licencia lo sigue rechazando el objeto de valor")
    void registrarConLicenciaMalFormada() {
        assertThatThrownBy(() -> servicio.registrar(new RegistrarConductorRequest(
                "Juan Perez Vasquez", "12345678", CategoriaDeLicencia.A_IIIB,
                LocalDate.of(2025, 1, 1), LocalDate.of(2027, 1, 1))))
                .isInstanceOf(NumeroDeLicenciaInvalidoException.class);
    }

    @Test
    @DisplayName("un conductor inexistente se traduce a RecursoNoEncontrado, nunca a un Optional vacio")
    void conductorInexistente() {
        when(repositorio.findById("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.porId("no-existe"))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("no-existe");
    }

    @Test
    @DisplayName("listar sin filtro devuelve todos; con filtro delega en el repositorio")
    void listarConYSinFiltro() {
        when(repositorio.findAll()).thenReturn(List.of(conductorVigente()));
        when(repositorio.findByEstadoSituacion(SituacionDeHabilitacion.SUSPENDIDO)).thenReturn(List.of());

        assertThat(servicio.listar(null)).hasSize(1);
        assertThat(servicio.listar(SituacionDeHabilitacion.SUSPENDIDO)).isEmpty();
    }

    @Test
    @DisplayName("el descanso reabre la ventana del dia del reloj y deja las horas en cero")
    void descansoReabreLaVentana() {
        Conductor conductor = conductorVigente();
        conductor.acumularHoras(new BigDecimal("7.50"), HOY);
        when(repositorio.findById("c-1")).thenReturn(Optional.of(conductor));

        var respuesta = servicio.registrarDescanso("c-1");

        assertThat(respuesta.horas().acumuladas()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(respuesta.horas().ventanaDesde()).isEqualTo(HOY);
    }

    @Test
    @DisplayName("rehabilitar con la licencia vencida deja subir la excepcion del dominio sin transformarla")
    void rehabilitarConLicenciaVencida() {
        Conductor conductor = new Conductor(
                "c-2", "Maria Lopez Diaz",
                new NumeroDeLicencia("Q99999999"),
                CategoriaDeLicencia.A_IIIA,
                new PeriodoDeVigencia(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1)),
                HorasDeConduccion.ventanaDe(HOY),
                EstadoDeHabilitacion.suspendido("licencia vencida"),
                List.of());
        when(repositorio.findById("c-2")).thenReturn(Optional.of(conductor));

        assertThatThrownBy(() -> servicio.rehabilitar("c-2"))
                .isInstanceOf(RehabilitacionInvalidaException.class);
    }

    @Test
    @DisplayName("suspender guarda el motivo que exige EstadoDeHabilitacion")
    void suspenderGuardaElMotivo() {
        when(repositorio.findById("c-1")).thenReturn(Optional.of(conductorVigente()));

        var respuesta = servicio.suspender("c-1", new SuspenderConductorRequest("papeleta pendiente"));

        assertThat(respuesta.situacion()).isEqualTo(SituacionDeHabilitacion.SUSPENDIDO);
        assertThat(respuesta.motivo()).isEqualTo("papeleta pendiente");
    }

    @Test
    @DisplayName("registrar una induccion reemplaza la del mismo cliente en vez de acumularla")
    void induccionPorClienteEsUnica() {
        Conductor conductor = conductorVigente();
        conductor.registrarInduccion(new Induccion("i-1", "cli-9",
                new PeriodoDeVigencia(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 1))));
        when(repositorio.findById("c-1")).thenReturn(Optional.of(conductor));

        servicio.registrarInduccion("c-1", new RegistrarInduccionRequest(
                "cli-9", LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1)));

        assertThat(conductor.getInducciones()).hasSize(1);
        assertThat(conductor.getInducciones().getFirst().getVigencia().hasta())
                .isEqualTo(LocalDate.of(2027, 1, 1));
    }

    @Test
    @DisplayName("las alertas miden los dias contra el reloj inyectado")
    void alertasContraElReloj() {
        Conductor porVencer = new Conductor(
                "c-3", "Pedro Ruiz Silva",
                new NumeroDeLicencia("Q11111111"),
                CategoriaDeLicencia.A_IIIA,
                new PeriodoDeVigencia(LocalDate.of(2025, 1, 1), HOY.plusDays(10)),
                HorasDeConduccion.ventanaDe(HOY),
                EstadoDeHabilitacion.habilitado(),
                List.of());
        when(repositorio.findAll()).thenReturn(List.of(porVencer, conductorVigente()));

        var alertas = servicio.alertas(30);

        assertThat(alertas).hasSize(1);
        assertThat(alertas.getFirst().conductorId()).isEqualTo("c-3");
        assertThat(alertas.getFirst().diasRestantes()).isEqualTo(10);
    }
}
