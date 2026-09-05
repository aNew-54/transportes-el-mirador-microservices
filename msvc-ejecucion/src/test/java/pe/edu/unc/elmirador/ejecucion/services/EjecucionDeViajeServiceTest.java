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
    private EjecucionDeViajeService service;

    @BeforeEach
    void setUp() {
        repository = mock(EjecucionDeViajeRepository.class);
        clock = Clock.fixed(Instant.parse("2026-05-01T10:00:00Z"), ZoneId.of("America/Lima"));
        service = new EjecucionDeViajeService(repository, clock);
    }

    @Test
    @DisplayName("crear delega al agregado y persiste")
    void crear() {
        when(repository.existsById("v-1")).thenReturn(false);

        CrearEjecucionRequest request = new CrearEjecucionRequest("v-1", "u-1", 
                List.of(new ParadaRequest(1, "os-1", "Dir 1")));
        
        EjecucionDeViajeResponse response = service.crear(request);
        
        assertThat(response.viajeId()).isEqualTo("v-1");
        assertThat(response.estado()).isEqualTo(EstadoDeEjecucion.PENDIENTE);
        verify(repository).save(any(EjecucionDeViaje.class));
    }

    @Test
    @DisplayName("crear lanza 409 si ya existe")
    void crearConflicto() {
        when(repository.existsById("v-1")).thenReturn(true);

        CrearEjecucionRequest request = new CrearEjecucionRequest("v-1", "u-1", 
                List.of(new ParadaRequest(1, "os-1", "Dir 1")));

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(ConflictoDeRecursoException.class)
                .hasMessageContaining("Ya existe una ejecucion para el viaje");
    }

    @Test
    @DisplayName("registrarConformidad delega al agregado y persiste")
    void registrarConformidad() {
        EjecucionDeViaje ejecucion = new EjecucionDeViaje("v-1", "u-1", 
                List.of(new Parada(1, "os-1", "Dir 1")));
        when(repository.findById("v-1")).thenReturn(Optional.of(ejecucion));

        ConformidadRequest request = new ConformidadRequest(EstadoConformidad.FIRMADA, "Juan", "");
        service.registrarConformidad("v-1", 1, request);

        verify(repository).save(ejecucion);
    }
}
