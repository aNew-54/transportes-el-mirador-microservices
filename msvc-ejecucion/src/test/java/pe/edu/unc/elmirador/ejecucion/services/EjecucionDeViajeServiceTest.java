package pe.edu.unc.elmirador.ejecucion.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
import static org.mockito.ArgumentMatchers.eq;
import java.util.Arrays;
import java.util.Collections;
import java.time.OffsetDateTime;
import java.lang.reflect.RecordComponent;
import org.mockito.ArgumentCaptor;
import pe.edu.unc.elmirador.ejecucion.exceptions.DominioEjecucionException;
import pe.edu.unc.elmirador.ejecucion.exceptions.LiquidacionPendienteException;
import pe.edu.unc.elmirador.ejecucion.exceptions.FacturacionIntegrationException;
import pe.edu.unc.elmirador.ejecucion.models.entity.LiquidacionDeViaje;
import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoDeLiquidacion;
import pe.edu.unc.elmirador.ejecucion.models.entity.Incidencia;
import pe.edu.unc.elmirador.ejecucion.models.vo.TipoDeIncidencia;
import pe.edu.unc.elmirador.ejecucion.models.vo.Evidencia;
import pe.edu.unc.elmirador.ejecucion.models.vo.EsperaFacturable;
import pe.edu.unc.elmirador.ejecucion.models.entity.ConformidadDeEntrega;
import pe.edu.unc.elmirador.ejecucion.models.vo.ResultadoDeCheckList;
import pe.edu.unc.elmirador.ejecucion.clients.dto.KilometrajePeticion;
import pe.edu.unc.elmirador.ejecucion.clients.dto.FallaPeticion;
import pe.edu.unc.elmirador.ejecucion.clients.dto.HorasConduccionPeticion;
import pe.edu.unc.elmirador.ejecucion.clients.dto.EsperaPeticion;
import pe.edu.unc.elmirador.ejecucion.clients.dto.ConformidadPeticion;
import pe.edu.unc.elmirador.ejecucion.dto.request.ConceptoFacturableRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.HorasDeConductorRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.RegistrarEsperaRequest;

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

    @Test
    @DisplayName("[LIQ-04] cerrar con liquidacion ABIERTA lanza LiquidacionPendienteException y no llama gateways")
    void cerrarConLiquidacionPendiente() {
        EjecucionDeViaje ejecucion = new EjecucionDeViaje("v-1", "u-1", List.of("c-1"), List.of(new Parada(1, "os-1", "Dir 1")));
        ejecucion.registrarCheckList(new ResultadoDeCheckList(true, List.of(), OffsetDateTime.now(clock)));
        ejecucion.iniciar(OffsetDateTime.now(clock));
        ejecucion.registrarConformidad(1, new ConformidadDeEntrega("conf-1", "os-1", EstadoConformidad.FIRMADA, "Juan", OffsetDateTime.now(clock), ""));
        ejecucion.marcarEntregada(OffsetDateTime.now(clock));
        
        when(repository.findById("v-1")).thenReturn(Optional.of(ejecucion));
        
        LiquidacionDeViaje liqui = mock(LiquidacionDeViaje.class);
        when(liquidaciones.findByViajeIdAndEstadoNot("v-1", EstadoDeLiquidacion.APROBADA)).thenReturn(List.of(liqui));
        
        CerrarEjecucionRequest request = new CerrarEjecucionRequest(100, List.of(new HorasDeConductorRequest("c-1", 8.0, OffsetDateTime.now(clock), OffsetDateTime.now(clock))), List.of());
        
        assertThatThrownBy(() -> service.cerrar("v-1", request))
                .isInstanceOf(LiquidacionPendienteException.class);
                
        assertThat(ejecucion.getEstado()).isEqualTo(EstadoDeEjecucion.ENTREGADA);
        verify(unidadesGateway, never()).reportarKilometraje(any(), any());
        verify(unidadesGateway, never()).reportarFalla(any(), any(), any());
        verify(conductoresGateway, never()).reportarHoras(any(), any());
        verify(conductoresGateway, never()).reportarIncidencia(any(), any(), any());
        verify(comercialGateway, never()).reportarEspera(any(), any());
        verify(facturacionGateway, never()).registrarConformidad(any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("[LIQ-04] cerrar sin liquidaciones pendientes pasa a CERRADA")
    void cerrarSinLiquidacionesPendientes() {
        EjecucionDeViaje ejecucion = new EjecucionDeViaje("v-1", "u-1", List.of("c-1"), List.of(new Parada(1, "os-1", "Dir 1")));
        ejecucion.registrarCheckList(new ResultadoDeCheckList(true, List.of(), OffsetDateTime.now(clock)));
        ejecucion.iniciar(OffsetDateTime.now(clock));
        ejecucion.registrarConformidad(1, new ConformidadDeEntrega("conf-1", "os-1", EstadoConformidad.FIRMADA, "Juan", OffsetDateTime.now(clock), ""));
        ejecucion.marcarEntregada(OffsetDateTime.now(clock));
        
        when(repository.findById("v-1")).thenReturn(Optional.of(ejecucion));
        when(liquidaciones.findByViajeIdAndEstadoNot("v-1", EstadoDeLiquidacion.APROBADA)).thenReturn(List.of());
        
        CerrarEjecucionRequest request = new CerrarEjecucionRequest(100, List.of(new HorasDeConductorRequest("c-1", 8.0, OffsetDateTime.now(clock), OffsetDateTime.now(clock))), List.of());
        
        service.cerrar("v-1", request);
        
        assertThat(ejecucion.getEstado()).isEqualTo(EstadoDeEjecucion.CERRADA);
        verify(repository).save(ejecucion);
    }

    @Test
    @DisplayName("[LIQ-04] CerrarEjecucionRequest no expone datos de liquidacion por reflexion")
    void cerrarEjecucionRequestNoFalseable() {
        boolean tieneCampoLiquidacion = Arrays.stream(CerrarEjecucionRequest.class.getRecordComponents())
                .map(RecordComponent::getName)
                .anyMatch(nombre -> nombre.toLowerCase().contains("liquidacion"));
                
        assertThat(tieneCampoLiquidacion)
                .as("[LIQ-04] Si el campo vuelve al DTO, la invariante deja de poder fallar")
                .isFalse();
    }

    @Test
    @DisplayName("[LIQ-04] cerrar le pregunta al repositorio de liquidaciones, no al cuerpo")
    void cerrarConsultaElRepositorioDeLiquidaciones() {
        EjecucionDeViaje ejecucion = ejecucionEntregadaDeUnaParada();

        when(repository.findById("v-1")).thenReturn(Optional.of(ejecucion));
        when(liquidaciones.findByViajeIdAndEstadoNot("v-1", EstadoDeLiquidacion.APROBADA)).thenReturn(List.of());

        service.cerrar("v-1", new CerrarEjecucionRequest(100, List.of(
                new HorasDeConductorRequest("c-1", 8.0, OffsetDateTime.now(clock), OffsetDateTime.now(clock))),
                List.of()));

        // Que el DTO ya no lleve el booleano no basta: sin esta llamada el servicio podria estar
        // asumiendo que nunca hay pendientes, que es el mismo defecto con otra cara.
        verify(liquidaciones).findByViajeIdAndEstadoNot("v-1", EstadoDeLiquidacion.APROBADA);
    }

    private EjecucionDeViaje ejecucionEntregadaDeUnaParada() {
        EjecucionDeViaje ejecucion = new EjecucionDeViaje("v-1", "u-1", List.of("c-1"),
                List.of(new Parada(1, "os-1", "Dir 1")));
        ejecucion.registrarCheckList(new ResultadoDeCheckList(true, List.of(), OffsetDateTime.now(clock)));
        ejecucion.iniciar(OffsetDateTime.now(clock));
        ejecucion.registrarConformidad(1, new ConformidadDeEntrega("conf-1", "os-1",
                EstadoConformidad.FIRMADA, "Juan", OffsetDateTime.now(clock), ""));
        ejecucion.marcarEntregada(OffsetDateTime.now(clock));
        return ejecucion;
    }

    @Test
    @DisplayName("cerrar con un conductor no asignado lanza DominioEjecucionException")
    void cerrarConductorNoAsignado() {
        EjecucionDeViaje ejecucion = new EjecucionDeViaje("v-1", "u-1", List.of("c-1"), List.of(new Parada(1, "os-1", "Dir 1")));
        ejecucion.registrarCheckList(new ResultadoDeCheckList(true, List.of(), OffsetDateTime.now(clock)));
        ejecucion.iniciar(OffsetDateTime.now(clock));
        ejecucion.registrarConformidad(1, new ConformidadDeEntrega("conf-1", "os-1", EstadoConformidad.FIRMADA, "Juan", OffsetDateTime.now(clock), ""));
        ejecucion.marcarEntregada(OffsetDateTime.now(clock));
        
        when(repository.findById("v-1")).thenReturn(Optional.of(ejecucion));
        when(liquidaciones.findByViajeIdAndEstadoNot("v-1", EstadoDeLiquidacion.APROBADA)).thenReturn(List.of());
        
        CerrarEjecucionRequest request = new CerrarEjecucionRequest(100, List.of(
            new HorasDeConductorRequest("c-1", 8.0, OffsetDateTime.now(clock), OffsetDateTime.now(clock)),
            new HorasDeConductorRequest("c-extra", 8.0, OffsetDateTime.now(clock), OffsetDateTime.now(clock))
        ), List.of());
        
        assertThatThrownBy(() -> service.cerrar("v-1", request))
                .isInstanceOf(DominioEjecucionException.class);
    }

    @Test
    @DisplayName("cerrar con falta de horas de un conductor asignado lanza DominioEjecucionException")
    void cerrarConductorFaltante() {
        EjecucionDeViaje ejecucion = new EjecucionDeViaje("v-1", "u-1", List.of("c-1", "c-2"), List.of(new Parada(1, "os-1", "Dir 1")));
        ejecucion.registrarCheckList(new ResultadoDeCheckList(true, List.of(), OffsetDateTime.now(clock)));
        ejecucion.iniciar(OffsetDateTime.now(clock));
        ejecucion.registrarConformidad(1, new ConformidadDeEntrega("conf-1", "os-1", EstadoConformidad.FIRMADA, "Juan", OffsetDateTime.now(clock), ""));
        ejecucion.marcarEntregada(OffsetDateTime.now(clock));
        
        when(repository.findById("v-1")).thenReturn(Optional.of(ejecucion));
        when(liquidaciones.findByViajeIdAndEstadoNot("v-1", EstadoDeLiquidacion.APROBADA)).thenReturn(List.of());
        
        CerrarEjecucionRequest request = new CerrarEjecucionRequest(100, List.of(
            new HorasDeConductorRequest("c-1", 8.0, OffsetDateTime.now(clock), OffsetDateTime.now(clock))
        ), List.of());
        
        assertThatThrownBy(() -> service.cerrar("v-1", request))
                .isInstanceOf(DominioEjecucionException.class);
    }

    @Test
    @DisplayName("cerrar con concepto facturable en orden ajena lanza DominioEjecucionException")
    void cerrarConceptoOrdenAjena() {
        EjecucionDeViaje ejecucion = new EjecucionDeViaje("v-1", "u-1", List.of("c-1"), List.of(new Parada(1, "os-1", "Dir 1")));
        ejecucion.registrarCheckList(new ResultadoDeCheckList(true, List.of(), OffsetDateTime.now(clock)));
        ejecucion.iniciar(OffsetDateTime.now(clock));
        ejecucion.registrarConformidad(1, new ConformidadDeEntrega("conf-1", "os-1", EstadoConformidad.FIRMADA, "Juan", OffsetDateTime.now(clock), ""));
        ejecucion.marcarEntregada(OffsetDateTime.now(clock));
        
        when(repository.findById("v-1")).thenReturn(Optional.of(ejecucion));
        when(liquidaciones.findByViajeIdAndEstadoNot("v-1", EstadoDeLiquidacion.APROBADA)).thenReturn(List.of());
        
        CerrarEjecucionRequest request = new CerrarEjecucionRequest(100, List.of(
            new HorasDeConductorRequest("c-1", 8.0, OffsetDateTime.now(clock), OffsetDateTime.now(clock))
        ), List.of(new ConceptoFacturableRequest("os-ajena", "PEAJE", "100.00", "PEN", "Peaje")));
        
        assertThatThrownBy(() -> service.cerrar("v-1", request))
                .isInstanceOf(DominioEjecucionException.class);
    }

    @Test
    @DisplayName("cerrar feliz empuja los cuatro contratos")
    void cerrarFelizEmpujaContratos() {
        EjecucionDeViaje ejecucion = new EjecucionDeViaje("v-1", "u-1", List.of("c-1", "c-2"), List.of(
            new Parada(1, "os-1", "Dir 1"),
            new Parada(2, "os-2", "Dir 2")
        ));
        ejecucion.registrarCheckList(new ResultadoDeCheckList(true, List.of(), OffsetDateTime.now(clock)));
        ejecucion.iniciar(OffsetDateTime.now(clock));
        ejecucion.registrarIncidencia(new Incidencia("inc-1", TipoDeIncidencia.AVERIA, "Falla motor", null, OffsetDateTime.now(clock)));
        ejecucion.registrarEspera(1, new EsperaFacturable(OffsetDateTime.now(clock), OffsetDateTime.now(clock).plusHours(4), 2));
        ejecucion.registrarConformidad(1, new ConformidadDeEntrega("conf-1", "os-1", EstadoConformidad.FIRMADA, "Juan", OffsetDateTime.now(clock), ""));
        ejecucion.registrarConformidad(2, new ConformidadDeEntrega("conf-2", "os-2", EstadoConformidad.FIRMADA, "Ana", OffsetDateTime.now(clock), ""));
        ejecucion.marcarEntregada(OffsetDateTime.now(clock));
        
        when(repository.findById("v-1")).thenReturn(Optional.of(ejecucion));
        when(liquidaciones.findByViajeIdAndEstadoNot("v-1", EstadoDeLiquidacion.APROBADA)).thenReturn(List.of());
        
        CerrarEjecucionRequest request = new CerrarEjecucionRequest(1500, List.of(
            new HorasDeConductorRequest("c-1", 8.0, OffsetDateTime.now(clock), OffsetDateTime.now(clock)),
            new HorasDeConductorRequest("c-2", 6.0, OffsetDateTime.now(clock), OffsetDateTime.now(clock))
        ), List.of());
        
        service.cerrar("v-1", request);
        
        ArgumentCaptor<KilometrajePeticion> kmCaptor = ArgumentCaptor.forClass(KilometrajePeticion.class);
        verify(unidadesGateway).reportarKilometraje(eq("u-1"), kmCaptor.capture());
        assertThat(kmCaptor.getValue().kilometraje()).isEqualTo(1500);
        assertThat(kmCaptor.getValue().momento()).isEqualTo(ejecucion.getFechaEntrega());
        
        ArgumentCaptor<FallaPeticion> fallaCaptor = ArgumentCaptor.forClass(FallaPeticion.class);
        verify(unidadesGateway).reportarFalla(eq("u-1"), eq("inc-1"), fallaCaptor.capture());
        assertThat(fallaCaptor.getValue().dejaInoperativa()).isTrue();
        
        ArgumentCaptor<HorasConduccionPeticion> horasCaptor = ArgumentCaptor.forClass(HorasConduccionPeticion.class);
        verify(conductoresGateway).reportarHoras(eq("c-1"), horasCaptor.capture());
        verify(conductoresGateway).reportarHoras(eq("c-2"), horasCaptor.capture());
        assertThat(horasCaptor.getAllValues()).hasSize(2);
        
        ArgumentCaptor<EsperaPeticion> esperaCaptor = ArgumentCaptor.forClass(EsperaPeticion.class);
        verify(comercialGateway).reportarEspera(eq("os-1"), esperaCaptor.capture());
        assertThat(esperaCaptor.getValue().viajeId()).isEqualTo("v-1");
        
        ArgumentCaptor<ConformidadPeticion> confCaptor = ArgumentCaptor.forClass(ConformidadPeticion.class);
        verify(facturacionGateway, times(2)).registrarConformidad(confCaptor.capture());
        assertThat(confCaptor.getAllValues()).hasSize(2);
    }

    @Test
    @DisplayName("incidencia averia resuelta se reporta con dejaInoperativa=false")
    void cerrarAveriaResuelta() {
        EjecucionDeViaje ejecucion = new EjecucionDeViaje("v-1", "u-1", List.of("c-1"), List.of(new Parada(1, "os-1", "Dir 1")));
        ejecucion.registrarCheckList(new ResultadoDeCheckList(true, List.of(), OffsetDateTime.now(clock)));
        ejecucion.iniciar(OffsetDateTime.now(clock));
        Incidencia averia = new Incidencia("inc-1", TipoDeIncidencia.AVERIA, "Falla", null, OffsetDateTime.now(clock));
        averia.resolver();
        ejecucion.registrarIncidencia(averia);
        ejecucion.registrarConformidad(1, new ConformidadDeEntrega("conf-1", "os-1", EstadoConformidad.FIRMADA, "Juan", OffsetDateTime.now(clock), ""));
        ejecucion.marcarEntregada(OffsetDateTime.now(clock));
        
        when(repository.findById("v-1")).thenReturn(Optional.of(ejecucion));
        when(liquidaciones.findByViajeIdAndEstadoNot("v-1", EstadoDeLiquidacion.APROBADA)).thenReturn(List.of());
        
        CerrarEjecucionRequest request = new CerrarEjecucionRequest(100, List.of(new HorasDeConductorRequest("c-1", 8.0, OffsetDateTime.now(clock), OffsetDateTime.now(clock))), List.of());
        
        service.cerrar("v-1", request);
        
        ArgumentCaptor<FallaPeticion> fallaCaptor = ArgumentCaptor.forClass(FallaPeticion.class);
        verify(unidadesGateway).reportarFalla(eq("u-1"), eq("inc-1"), fallaCaptor.capture());
        assertThat(fallaCaptor.getValue().dejaInoperativa()).isFalse();
    }

    @Test
    @DisplayName("conceptos facturables agrupados por orden en conformidad peticion")
    void cerrarConceptosAgrupados() {
        EjecucionDeViaje ejecucion = new EjecucionDeViaje("v-1", "u-1", List.of("c-1"), List.of(
            new Parada(1, "os-1", "Dir 1"),
            new Parada(2, "os-2", "Dir 2")
        ));
        ejecucion.registrarCheckList(new ResultadoDeCheckList(true, List.of(), OffsetDateTime.now(clock)));
        ejecucion.iniciar(OffsetDateTime.now(clock));
        ejecucion.registrarConformidad(1, new ConformidadDeEntrega("conf-1", "os-1", EstadoConformidad.FIRMADA, "Juan", OffsetDateTime.now(clock), ""));
        ejecucion.registrarConformidad(2, new ConformidadDeEntrega("conf-2", "os-2", EstadoConformidad.FIRMADA, "Ana", OffsetDateTime.now(clock), ""));
        ejecucion.marcarEntregada(OffsetDateTime.now(clock));
        
        when(repository.findById("v-1")).thenReturn(Optional.of(ejecucion));
        when(liquidaciones.findByViajeIdAndEstadoNot("v-1", EstadoDeLiquidacion.APROBADA)).thenReturn(List.of());
        
        CerrarEjecucionRequest request = new CerrarEjecucionRequest(100, List.of(
            new HorasDeConductorRequest("c-1", 8.0, OffsetDateTime.now(clock), OffsetDateTime.now(clock))
        ), List.of(
            new ConceptoFacturableRequest("os-1", "C1", "10.00", "PEN", "d1"),
            new ConceptoFacturableRequest("os-2", "C2", "20.00", "PEN", "d2"),
            new ConceptoFacturableRequest("os-2", "C3", "30.00", "PEN", "d3")
        ));
        
        service.cerrar("v-1", request);
        
        ArgumentCaptor<ConformidadPeticion> confCaptor = ArgumentCaptor.forClass(ConformidadPeticion.class);
        verify(facturacionGateway, times(2)).registrarConformidad(confCaptor.capture());
        
        List<ConformidadPeticion> peticiones = confCaptor.getAllValues();
        ConformidadPeticion p1 = peticiones.stream().filter(p -> p.ordenDeServicioId().equals("os-1")).findFirst().get();
        ConformidadPeticion p2 = peticiones.stream().filter(p -> p.ordenDeServicioId().equals("os-2")).findFirst().get();
        
        assertThat(p1.conceptosFacturables()).hasSize(1);
        assertThat(p2.conceptosFacturables()).hasSize(2);
    }

    @Test
    @DisplayName("incidenciasSinResolver enviadas a facturacion independientemente del cuerpo")
    void cerrarIncidenciasSinResolver() {
        EjecucionDeViaje ejecucion = new EjecucionDeViaje("v-1", "u-1", List.of("c-1"), List.of(
            new Parada(1, "os-1", "Dir 1"),
            new Parada(2, "os-2", "Dir 2")
        ));
        ejecucion.registrarCheckList(new ResultadoDeCheckList(true, List.of(), OffsetDateTime.now(clock)));
        ejecucion.iniciar(OffsetDateTime.now(clock));
        Evidencia evidencia = new Evidencia(List.of("foto"), "desc", OffsetDateTime.now(clock));
        ejecucion.registrarIncidencia(new Incidencia("inc-1", TipoDeIncidencia.DANIO, "Danio", evidencia, OffsetDateTime.now(clock)));
        ejecucion.registrarConformidad(1, new ConformidadDeEntrega("conf-1", "os-1", EstadoConformidad.FIRMADA, "Juan", OffsetDateTime.now(clock), ""));
        ejecucion.registrarConformidad(2, new ConformidadDeEntrega("conf-2", "os-2", EstadoConformidad.FIRMADA, "Ana", OffsetDateTime.now(clock), ""));
        ejecucion.marcarEntregada(OffsetDateTime.now(clock));
        
        when(repository.findById("v-1")).thenReturn(Optional.of(ejecucion));
        when(liquidaciones.findByViajeIdAndEstadoNot("v-1", EstadoDeLiquidacion.APROBADA)).thenReturn(List.of());
        
        CerrarEjecucionRequest request = new CerrarEjecucionRequest(100, List.of(
            new HorasDeConductorRequest("c-1", 8.0, OffsetDateTime.now(clock), OffsetDateTime.now(clock))
        ), List.of());
        
        service.cerrar("v-1", request);
        
        ArgumentCaptor<ConformidadPeticion> confCaptor = ArgumentCaptor.forClass(ConformidadPeticion.class);
        verify(facturacionGateway, times(2)).registrarConformidad(confCaptor.capture());
        
        for (ConformidadPeticion p : confCaptor.getAllValues()) {
            assertThat(p.incidenciasSinResolver()).containsExactly("inc-1");
        }
    }

    @Test
    @DisplayName("si registrarConformidad lanza FacturacionIntegrationException, cerrar la propaga")
    void cerrarPropagaExcepcionGateway() {
        EjecucionDeViaje ejecucion = new EjecucionDeViaje("v-1", "u-1", List.of("c-1"), List.of(new Parada(1, "os-1", "Dir 1")));
        ejecucion.registrarCheckList(new ResultadoDeCheckList(true, List.of(), OffsetDateTime.now(clock)));
        ejecucion.iniciar(OffsetDateTime.now(clock));
        ejecucion.registrarConformidad(1, new ConformidadDeEntrega("conf-1", "os-1", EstadoConformidad.FIRMADA, "Juan", OffsetDateTime.now(clock), ""));
        ejecucion.marcarEntregada(OffsetDateTime.now(clock));
        
        when(repository.findById("v-1")).thenReturn(Optional.of(ejecucion));
        when(liquidaciones.findByViajeIdAndEstadoNot("v-1", EstadoDeLiquidacion.APROBADA)).thenReturn(List.of());
        // registrarConformidad devuelve void: with when(...) no compila.
        org.mockito.Mockito.doThrow(new FacturacionIntegrationException("Facturacion no respondio"))
                .when(facturacionGateway).registrarConformidad(any());
        
        CerrarEjecucionRequest request = new CerrarEjecucionRequest(100, List.of(
            new HorasDeConductorRequest("c-1", 8.0, OffsetDateTime.now(clock), OffsetDateTime.now(clock))
        ), List.of());
        
        assertThatThrownBy(() -> service.cerrar("v-1", request))
                .isInstanceOf(FacturacionIntegrationException.class);
    }

    @Test
    @DisplayName("registrarEspera delega en el agregado y persiste")
    void registrarEspera() {
        EjecucionDeViaje ejecucion = new EjecucionDeViaje("v-1", "u-1", List.of("c-1"), List.of(new Parada(1, "os-1", "Dir 1")));
        when(repository.findById("v-1")).thenReturn(Optional.of(ejecucion));
        
        RegistrarEsperaRequest request = new RegistrarEsperaRequest(OffsetDateTime.now(clock), OffsetDateTime.now(clock).plusHours(2), 1);
        service.registrarEspera("v-1", 1, request);
        
        verify(repository).save(ejecucion);
        assertThat(ejecucion.paradasConEspera()).hasSize(1);
    }
}
