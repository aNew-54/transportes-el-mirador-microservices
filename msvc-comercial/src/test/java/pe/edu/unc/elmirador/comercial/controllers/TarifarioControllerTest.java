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

import pe.edu.unc.elmirador.comercial.dto.request.RegistrarTarifarioRequest;
import pe.edu.unc.elmirador.comercial.dto.response.TarifarioResponse;
import pe.edu.unc.elmirador.comercial.exceptions.TarifarioVigenteDuplicadoException;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeRecargo;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeUnidad;
import pe.edu.unc.elmirador.comercial.services.TarifarioService;

@WebMvcTest(TarifarioController.class)
class TarifarioControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TarifarioService servicio;

    @Test
    void publicar_tarifarioValido_devuelve201() throws Exception {
        RegistrarTarifarioRequest request = new RegistrarTarifarioRequest(
                LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-12-31"),
                List.of(new RegistrarTarifarioRequest.PrecioDeTarifarioRequest(
                        "LIMA", "PIURA", "NORTE", TipoDeUnidad.FURGON, new BigDecimal("1000.00"), "PEN")),
                List.of(new RegistrarTarifarioRequest.RecargoRequest(
                        TipoDeRecargo.COMBUSTIBLE, new BigDecimal("10.00")))
        );

        TarifarioResponse response = new TarifarioResponse(
                "tar-123",
                LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-12-31"),
                List.of(new TarifarioResponse.PrecioDeTarifarioResponse(
                        "prec-1", "LIMA", "PIURA", "NORTE", "FURGON", new BigDecimal("1000.00"), "PEN")),
                List.of(new TarifarioResponse.RecargoResponse(
                        "COMBUSTIBLE", new BigDecimal("10.00")))
        );

        when(servicio.publicar(any())).thenReturn(response);

        mvc.perform(post("/api/v1/tarifarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/tarifarios/tar-123"))
                .andExpect(jsonPath("$.id").value("tar-123"));
    }

    @Test
    void publicar_vigenciasSolapadas_devuelve409() throws Exception {
        RegistrarTarifarioRequest request = new RegistrarTarifarioRequest(
                LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-12-31"),
                List.of(),
                List.of()
        );

        when(servicio.publicar(any())).thenThrow(new TarifarioVigenteDuplicadoException("Solapamiento"));

        mvc.perform(post("/api/v1/tarifarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/tarifario-vigente-duplicado"))
                .andExpect(jsonPath("$.status").value(409));
    }
}
