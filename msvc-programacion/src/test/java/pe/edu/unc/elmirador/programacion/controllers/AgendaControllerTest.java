package pe.edu.unc.elmirador.programacion.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.unc.elmirador.programacion.dto.response.AgendaDeConductorResponse;
import pe.edu.unc.elmirador.programacion.dto.response.AgendaDeUnidadResponse;
import pe.edu.unc.elmirador.programacion.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.programacion.services.AgendaService;

@WebMvcTest(AgendaController.class)
class AgendaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AgendaService servicio;

    @Test
    @DisplayName("GET /agendas/unidades/{unidadId} devuelve 200")
    void porUnidadId200() throws Exception {
        when(servicio.consultarAgendaDeUnidad("u-1")).thenReturn(new AgendaDeUnidadResponse("u-1", List.of()));

        mockMvc.perform(get("/api/v1/agendas/unidades/u-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unidadId").value("u-1"));
    }

    @Test
    @DisplayName("GET /agendas/unidades/{unidadId} 404")
    void porUnidadId404() throws Exception {
        when(servicio.consultarAgendaDeUnidad("no-existe")).thenThrow(new RecursoNoEncontradoException("AgendaDeUnidad", "no-existe"));

        mockMvc.perform(get("/api/v1/agendas/unidades/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/recurso-no-encontrado"));
    }

    @Test
    @DisplayName("GET /agendas/conductores/{conductorId} devuelve 200")
    void porConductorId200() throws Exception {
        when(servicio.consultarAgendaDeConductor("c-1")).thenReturn(new AgendaDeConductorResponse("c-1", List.of()));

        mockMvc.perform(get("/api/v1/agendas/conductores/c-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conductorId").value("c-1"));
    }

    @Test
    @DisplayName("GET /agendas/conductores/{conductorId} 404")
    void porConductorId404() throws Exception {
        when(servicio.consultarAgendaDeConductor("no-existe")).thenThrow(new RecursoNoEncontradoException("AgendaDeConductor", "no-existe"));

        mockMvc.perform(get("/api/v1/agendas/conductores/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/recurso-no-encontrado"));
    }
}
