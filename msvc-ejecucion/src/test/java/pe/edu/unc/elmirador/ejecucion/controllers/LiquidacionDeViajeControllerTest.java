package pe.edu.unc.elmirador.ejecucion.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.edu.unc.elmirador.ejecucion.dto.request.AbrirLiquidacionRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.RendirGastoRequest;
import pe.edu.unc.elmirador.ejecucion.dto.response.LiquidacionDeViajeResponse;
import pe.edu.unc.elmirador.ejecucion.exceptions.GastoSinComprobanteException;
import pe.edu.unc.elmirador.ejecucion.exceptions.LiquidacionAprobadaException;
import pe.edu.unc.elmirador.ejecucion.models.vo.ConceptoDeGasto;
import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoDeLiquidacion;
import pe.edu.unc.elmirador.ejecucion.services.LiquidacionDeViajeService;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(LiquidacionDeViajeController.class)
class LiquidacionDeViajeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @MockitoBean
    private LiquidacionDeViajeService servicio;

    private LiquidacionDeViajeResponse respuestaDeEjemplo() {
        return new LiquidacionDeViajeResponse(
                "v-1", "c-1", new BigDecimal("100.00"), "PEN",
                List.of(), EstadoDeLiquidacion.ABIERTA, null,
                new BigDecimal("100.00"), "PEN", "A_FAVOR_DE_LA_EMPRESA");
    }

    @Test
    @DisplayName("POST /liquidaciones devuelve 201 con Location")
    void abrir201() throws Exception {
        when(servicio.abrir(any())).thenReturn(respuestaDeEjemplo());

        AbrirLiquidacionRequest peticion = new AbrirLiquidacionRequest(
                "v-1", "c-1", new BigDecimal("100.00"), "PEN");

        mockMvc.perform(post("/api/v1/liquidaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(peticion)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/liquidaciones/v-1/c-1"));
    }

    @Test
    @DisplayName("POST /liquidaciones/{viajeId}/{conductorId}/gastos devuelve 201")
    void rendirGasto201() throws Exception {
        when(servicio.rendirGasto(eq("v-1"), eq("c-1"), any())).thenReturn(respuestaDeEjemplo());

        RendirGastoRequest peticion = new RendirGastoRequest(
                ConceptoDeGasto.COMBUSTIBLE, new BigDecimal("50.00"), "PEN",
                "Factura", "F001-123", OffsetDateTime.now(), "Combustible");

        mockMvc.perform(post("/api/v1/liquidaciones/v-1/c-1/gastos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(peticion)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /liquidaciones/{viajeId}/{conductorId}/gastos sin comprobante devuelve 422")
    void rendirGasto422() throws Exception {
        when(servicio.rendirGasto(eq("v-1"), eq("c-1"), any()))
                .thenThrow(new GastoSinComprobanteException("Sin comprobante"));

        RendirGastoRequest peticion = new RendirGastoRequest(
                ConceptoDeGasto.COMBUSTIBLE, new BigDecimal("50.00"), "PEN",
                "Factura", "F001-123", OffsetDateTime.now(), "Combustible");

        mockMvc.perform(post("/api/v1/liquidaciones/v-1/c-1/gastos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(peticion)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/gasto-sin-comprobante"));
    }

    @Test
    @DisplayName("POST /liquidaciones/{viajeId}/{conductorId}/aprobar devuelve 200")
    void aprobar200() throws Exception {
        when(servicio.aprobar("v-1", "c-1")).thenReturn(respuestaDeEjemplo());

        mockMvc.perform(post("/api/v1/liquidaciones/v-1/c-1/aprobar"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /liquidaciones/{viajeId}/{conductorId}/aprobar en liquidacion aprobada devuelve 409")
    void aprobar409() throws Exception {
        when(servicio.aprobar("v-1", "c-1")).thenThrow(new LiquidacionAprobadaException("Ya aprobada"));

        mockMvc.perform(post("/api/v1/liquidaciones/v-1/c-1/aprobar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/liquidacion-aprobada"));
    }
}
