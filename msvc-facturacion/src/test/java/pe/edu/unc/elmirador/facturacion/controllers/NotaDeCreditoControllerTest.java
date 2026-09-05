package pe.edu.unc.elmirador.facturacion.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.edu.unc.elmirador.facturacion.dto.response.NotaDeCreditoResponse;
import pe.edu.unc.elmirador.facturacion.exceptions.MontoExcedeElSaldoException;
import pe.edu.unc.elmirador.facturacion.models.vo.MotivoDeAjuste;
import pe.edu.unc.elmirador.facturacion.services.NotaDeCreditoService;

@WebMvcTest(NotaDeCreditoController.class)
class NotaDeCreditoControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private NotaDeCreditoService servicio;

    @Test
    @DisplayName("POST /notas-de-credito devuelve 201")
    void emitir201() throws Exception {
        NotaDeCreditoResponse resp = new NotaDeCreditoResponse(
            "NC-1", "fac-1", MotivoDeAjuste.ERROR_DE_FACTURACION, new BigDecimal("100.00"), "PEN", OffsetDateTime.now(), ""
        );
        when(servicio.emitir(any())).thenReturn(resp);

        String json = """
            {"facturaId":"fac-1","motivo":"ERROR_DE_FACTURACION","monto":100.00}
            """;

        mockMvc.perform(post("/api/v1/notas-de-credito")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/notas-de-credito/NC-1"));
    }

    @Test
    @DisplayName("POST /notas-de-credito cuando el monto excede el saldo devuelve 422")
    void emitir422() throws Exception {
        when(servicio.emitir(any())).thenThrow(new MontoExcedeElSaldoException("El monto excede el saldo"));

        String json = """
            {"facturaId":"fac-1","motivo":"ERROR_DE_FACTURACION","monto":100.00}
            """;

        mockMvc.perform(post("/api/v1/notas-de-credito")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/monto-excede-el-saldo"));
    }
}
