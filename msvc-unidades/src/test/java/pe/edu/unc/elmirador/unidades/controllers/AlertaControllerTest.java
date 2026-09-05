package pe.edu.unc.elmirador.unidades.controllers;

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

import pe.edu.unc.elmirador.unidades.dto.response.AlertaResponse;
import pe.edu.unc.elmirador.unidades.services.UnidadService;

@WebMvcTest(AlertaController.class)
class AlertaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UnidadService servicio;

    @Test
    @DisplayName("GET /alertas devuelve 200 y lista de alertas")
    void alertas200() throws Exception {
        when(servicio.alertas()).thenReturn(List.of(
                new AlertaResponse("u-1", "ABC-123", AlertaResponse.TipoDeAlerta.DOCUMENTO_POR_VENCER, "SOAT", "Vence pronto")
        ));

        mockMvc.perform(get("/api/v1/alertas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
