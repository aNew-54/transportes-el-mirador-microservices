package pe.edu.unc.elmirador.cobranza.controllers;

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

import pe.edu.unc.elmirador.cobranza.services.CuentaCorrienteService;

@WebMvcTest(CarteraController.class)
class CarteraControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CuentaCorrienteService servicio;

    @Test
    @DisplayName("GET /cartera/gestion devuelve 200")
    void gestion200() throws Exception {
        when(servicio.carteraGestion()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/cartera/gestion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
