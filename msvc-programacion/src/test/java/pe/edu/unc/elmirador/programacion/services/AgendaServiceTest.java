package pe.edu.unc.elmirador.programacion.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.programacion.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.programacion.repositories.AgendaDeConductorRepository;
import pe.edu.unc.elmirador.programacion.repositories.AgendaDeUnidadRepository;

class AgendaServiceTest {

    private AgendaDeUnidadRepository agendaDeUnidadRepository;
    private AgendaDeConductorRepository agendaDeConductorRepository;
    private AgendaService servicio;

    @BeforeEach
    void preparar() {
        agendaDeUnidadRepository = mock(AgendaDeUnidadRepository.class);
        agendaDeConductorRepository = mock(AgendaDeConductorRepository.class);
        servicio = new AgendaService(agendaDeUnidadRepository, agendaDeConductorRepository);
    }

    @Test
    @DisplayName("consultarAgendaDeUnidad para unidad inexistente lanza RecursoNoEncontradoException")
    void agendaUnidadNoEncontrada() {
        when(agendaDeUnidadRepository.findById("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.consultarAgendaDeUnidad("no-existe"))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("no-existe");
    }

    @Test
    @DisplayName("consultarAgendaDeConductor para conductor inexistente lanza RecursoNoEncontradoException")
    void agendaConductorNoEncontrada() {
        when(agendaDeConductorRepository.findById("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.consultarAgendaDeConductor("no-existe"))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("no-existe");
    }
}
