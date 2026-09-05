package pe.edu.unc.elmirador.ejecucion.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pe.edu.unc.elmirador.ejecucion.dto.request.CerrarEjecucionRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.ConformidadRequest;
import static org.mockito.Mockito.never;
import pe.edu.unc.elmirador.ejecucion.exceptions.ProgramacionIntegrationException;
import pe.edu.unc.elmirador.ejecucion.clients.HojaDeRutaDeViaje;
import pe.edu.unc.elmirador.ejecucion.clients.ComercialGateway;
import pe.edu.unc.elmirador.ejecucion.clients.ConductoresGateway;
import pe.edu.unc.elmirador.ejecucion.clients.FacturacionGateway;
import pe.edu.unc.elmirador.ejecucion.clients.ProgramacionGateway;
import pe.edu.unc.elmirador.ejecucion.clients.UnidadesGateway;
import pe.edu.unc.elmirador.ejecucion.repositories.LiquidacionDeViajeRepository;
import pe.edu.unc.elmirador.ejecucion.dto.request.CrearEjecucionRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.ParadaRequest;
import pe.edu.unc.elmirador.ejecucion.dto.response.EjecucionDeViajeResponse;
import pe.edu.unc.elmirador.ejecucion.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.ejecucion.models.entity.EjecucionDeViaje;
import pe.edu.unc.elmirador.ejecucion.models.entity.Parada;
import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoConformidad;
import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoDeEjecucion;
import pe.edu.unc.elmirador.ejecucion.repositories.EjecucionDeViajeRepository;

class EjecucionDeViajeServiceTest {

    private EjecucionDeViajeRepository repository;
    private Clock clock;
    private ProgramacionGateway programacionGateway;
    private LiquidacionDeViajeRepository liquidaciones;
    private UnidadesGateway unidadesGateway;
    private ConductoresGateway conductoresGateway;
    private ComercialGateway comercialGateway;
    private FacturacionGateway facturacionGateway;
    private EjecucionDeViajeService service;

    @BeforeEach
    void setUp() {
        repository = mock(EjecucionDeViajeRepository.class);
        clock = Clock.fixed(Instant.parse("2026-05-01T10:00:00Z"), ZoneId.of("America/Lima"));
        programacionGateway = mock(ProgramacionGateway.class);
        when(programacionGateway.obtenerHojaDeRuta(any())).thenReturn(new HojaDeRutaDeViaje(
                "v-1", "DESPACHADO", "u-1", List.of("c-1"),
                List.of(new HojaDeRutaDeViaje.ParadaPlanificada(1, "os-1", "Dir 1"))));
        liquidaciones = mock(LiquidacionDeViajeRepository.class);
        unidadesGateway = mock(UnidadesGateway.class);
        conductoresGateway = mock(ConductoresGateway.class);
        comercialGateway = mock(ComercialGateway.class);
        facturacionGateway = mock(FacturacionGateway.class);
        service = new EjecucionDeViajeService(repository, liquidaciones, programacionGateway,
                unidadesGateway, conductoresGateway, comercialGateway, facturacionGateway, clock);
    }

    @Test
    @DisplayName("crear delega al agregado y persiste")
    void crear() {
        when(repository.existsById("v-1")).thenReturn(false);

        CrearEjecucionRequest request = new CrearEjecucionRequest("v-1");
        
        EjecucionDeViajeResponse response = service.crear(request);
        
        assertThat(response.viajeId()).isEqualTo("v-1");
        assertThat(response.estado()).isEqualTo(EstadoDeEjecucion.PENDIENTE);
        verify(repository).save(any(EjecucionDeViaje.class));
    }

    /**
     * Contrato 4. La unidad ejecutora y las paradas venian en el cuerpo de la peticion, asi que quien
     * abria la ejecucion podia declarar una unidad distinta de la programada y unas paradas que nadie
     * habia planificado. Ahora las trae la hoja de ruta.
     */
    @Test
    @DisplayName("crear toma la unidad y las paradas de la hoja de ruta, no de la peticion")
    void crearTomaLaHojaDeRutaDeProgramacion() {
        when(repository.existsById("v-1")).thenReturn(false);

        EjecucionDeViajeResponse respuesta = service.crear(new CrearEjecucionRequest("v-1"));

        verify(programacionGateway).obtenerHojaDeRuta("v-1");
        // La unidad sale de la hoja de ruta. La peticion ya no la lleva y no puede contradecirla.
        assertThat(respuesta.unidadEjecutoraId()).isEqualTo("u-1");
        verify(repository).save(any(EjecucionDeViaje.class));
    }

    /** Sin hoja de ruta confirmada no se abre la ejecucion. No se ejecuta un viaje a ciegas. */
    @Test
    @DisplayName("con Programacion caida no se abre la ejecucion")
    void crearConProgramacionCaidaNoPersisteNada() {
        when(repository.existsById("v-1")).thenReturn(false);
        when(programacionGateway.obtenerHojaDeRuta("v-1"))
                .thenThrow(new ProgramacionIntegrationException("Programacion no respondio"));

        assertThatThrownBy(() -> service.crear(new CrearEjecucionRequest("v-1")))
                .isInstanceOf(ProgramacionIntegrationException.class);

        verify(repository, never()).save(any(EjecucionDeViaje.class));
    }

    @Test
    @DisplayName("crear lanza 409 si ya existe")
    void crearConflicto() {
        when(repository.existsById("v-1")).thenReturn(true);

        CrearEjecucionRequest request = new CrearEjecucionRequest("v-1");

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(ConflictoDeRecursoException.class)
                .hasMessageContaining("Ya existe una ejecucion para el viaje");
    }

    @Test
    @DisplayName("registrarConformidad delega al agregado y persiste")
    void registrarConformidad() {
        EjecucionDeViaje ejecucion = new EjecucionDeViaje("v-1", "u-1", List.of("c-1"),
                List.of(new Parada(1, "os-1", "Dir 1")));
        when(repository.findById("v-1")).thenReturn(Optional.of(ejecucion));

        ConformidadRequest request = new ConformidadRequest(EstadoConformidad.FIRMADA, "Juan", "");
        service.registrarConformidad("v-1", 1, request);

        verify(repository).save(ejecucion);
    }
}
