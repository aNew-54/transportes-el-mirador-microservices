package pe.edu.unc.elmirador.unidades.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.edu.unc.elmirador.unidades.dto.request.AjustarInventarioRequest;
import pe.edu.unc.elmirador.unidades.dto.request.RegistrarRepuestoRequest;
import pe.edu.unc.elmirador.unidades.dto.response.RepuestoResponse;
import pe.edu.unc.elmirador.unidades.exceptions.ExistenciasNegativasException;
import pe.edu.unc.elmirador.unidades.services.RepuestoService;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(RepuestoController.class)
class RepuestoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @MockitoBean
    private RepuestoService servicio;

    private RepuestoResponse respuestaDeEjemplo() {
        return new RepuestoResponse("r-1", "F-001", "Filtro", 10, 5, new BigDecimal("10.00"), "PEN", false);
    }

    @Test
    @DisplayName("POST /repuestos devuelve 201")
    void registrar201() throws Exception {
        when(servicio.registrar(any())).thenReturn(respuestaDeEjemplo());

        mockMvc.perform(post("/api/v1/repuestos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RegistrarRepuestoRequest("F-001", "Filtro", 10, 5, new BigDecimal("10.00"), "PEN"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/repuestos/r-1"));
    }

    @Test
    @DisplayName("POST /repuestos/{id}/movimientos devuelve 200")
    void movimientos200() throws Exception {
        when(servicio.ajustarInventario(eq("r-1"), any())).thenReturn(respuestaDeEjemplo());

        mockMvc.perform(post("/api/v1/repuestos/r-1/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new AjustarInventarioRequest(5))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /repuestos/{id}/movimientos con negativo excedente devuelve 422")
    void movimientos422() throws Exception {
        when(servicio.ajustarInventario(eq("r-1"), any())).thenThrow(new ExistenciasNegativasException("Invalido"));

        mockMvc.perform(post("/api/v1/repuestos/r-1/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new AjustarInventarioRequest(-20))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/invariante-violada"));
    }
}
