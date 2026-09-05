package pe.edu.unc.elmirador.comercial.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import pe.edu.unc.elmirador.comercial.dto.request.RegistrarContratoMarcoRequest;
import pe.edu.unc.elmirador.comercial.dto.response.ContratoMarcoResponse;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeUnidad;
import pe.edu.unc.elmirador.comercial.services.ContratoMarcoService;

@WebMvcTest(ContratoMarcoController.class)
class ContratoMarcoControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ContratoMarcoService servicio;

    @Test
    void registrar_contratoValido_devuelve201() throws Exception {
        RegistrarContratoMarcoRequest request = new RegistrarContratoMarcoRequest(
                "cli-1", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"),
                48, true, List.of("NORTE"),
                List.of(new RegistrarContratoMarcoRequest.TarifaPactadaRequest(
                        "LIMA", "PIURA", "NORTE", TipoDeUnidad.FURGON, new BigDecimal("1000.00"), "PEN")));

        ContratoMarcoResponse response = new ContratoMarcoResponse(
                "ctm-1", "cli-1", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"),
                48, true, List.of("NORTE"),
                List.of(new ContratoMarcoResponse.TarifaPactadaResponse(
                        "tar-1", "LIMA", "PIURA", "NORTE", "FURGON", new BigDecimal("1000.00"), "PEN")));

        when(servicio.registrar(any())).thenReturn(response);

        mvc.perform(post("/api/v1/contratos-marco")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/contratos-marco/ctm-1"))
                .andExpect(jsonPath("$.id").value("ctm-1"));
    }

    @Test
    void registrar_peticionInvalida_devuelve400() throws Exception {
        // Falta clienteId (es @NotBlank)
        RegistrarContratoMarcoRequest request = new RegistrarContratoMarcoRequest(
                null, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"),
                48, true, List.of("NORTE"), List.of());

        mvc.perform(post("/api/v1/contratos-marco")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/validacion"))
                .andExpect(jsonPath("$.status").value(400));
    }
}
