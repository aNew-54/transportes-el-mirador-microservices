package pe.edu.unc.elmirador.programacion.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import pe.edu.unc.elmirador.programacion.dto.request.CargaRequest;
import pe.edu.unc.elmirador.programacion.dto.request.PlanificarViajeRequest;
import pe.edu.unc.elmirador.programacion.dto.request.RutaRequest;
import pe.edu.unc.elmirador.programacion.dto.request.VentanaDeTiempoRequest;
import pe.edu.unc.elmirador.programacion.dto.response.ViajeResponse;
import pe.edu.unc.elmirador.programacion.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.programacion.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.programacion.models.entity.Viaje;
import pe.edu.unc.elmirador.programacion.models.vo.TipoDeCarga;
import pe.edu.unc.elmirador.programacion.repositories.AgendaDeConductorRepository;
import pe.edu.unc.elmirador.programacion.repositories.AgendaDeUnidadRepository;
import pe.edu.unc.elmirador.programacion.repositories.ViajeRepository;

class ViajeServiceTest {

    private ViajeRepository viajeRepository;
    private AgendaDeUnidadRepository agendaDeUnidadRepository;
    private AgendaDeConductorRepository agendaDeConductorRepository;
    private ViajeService servicio;

    @BeforeEach
    void preparar() {
        viajeRepository = mock(ViajeRepository.class);
        agendaDeUnidadRepository = mock(AgendaDeUnidadRepository.class);
        agendaDeConductorRepository = mock(AgendaDeConductorRepository.class);
        servicio = new ViajeService(viajeRepository, agendaDeUnidadRepository, agendaDeConductorRepository);

        when(viajeRepository.save(any(Viaje.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("planificar viaje con id existente lanza ConflictoDeRecursoException")
    void planificarConConflicto() {
        when(viajeRepository.existsById("v-1")).thenReturn(true);

        PlanificarViajeRequest req = new PlanificarViajeRequest(
                "v-1",
                new RutaRequest("Lima", "Arequipa", "Sur"),
                new VentanaDeTiempoRequest(OffsetDateTime.parse("2026-03-10T10:00:00Z"), OffsetDateTime.parse("2026-03-11T10:00:00Z")),
                new CargaRequest("ord-1", 1000, new BigDecimal("2.5"), TipoDeCarga.PALETIZADA, 1)
        );

        assertThatThrownBy(() -> servicio.planificar(req))
                .isInstanceOf(ConflictoDeRecursoException.class);
    }

    @Test
    @DisplayName("consultar viaje inexistente lanza RecursoNoEncontradoException")
    void consultarNoExiste() {
        when(viajeRepository.findById("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.consultar("no-existe"))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("no-existe");
    }

    @Test
    @DisplayName("despachar delega en el agregado y guarda")
    void despacharDelega() {
        Viaje viajeMock = mock(Viaje.class);
        when(viajeMock.id()).thenReturn("v-1");
        when(viajeRepository.findById("v-1")).thenReturn(Optional.of(viajeMock));

        servicio.despachar("v-1");

        verify(viajeMock).autorizarDespacho();
        verify(viajeRepository).save(viajeMock);
    }
}
