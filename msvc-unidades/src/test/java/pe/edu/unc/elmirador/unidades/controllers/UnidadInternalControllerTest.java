package pe.edu.unc.elmirador.unidades.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.edu.unc.elmirador.unidades.dto.internal.request.ReportarFallaRequest;
import pe.edu.unc.elmirador.unidades.dto.internal.request.ReportarKilometrajeRequest;
import pe.edu.unc.elmirador.unidades.dto.internal.response.ElegibilidadUnidadResponse;
import pe.edu.unc.elmirador.unidades.dto.internal.response.FallaRegistradaResponse;
import pe.edu.unc.elmirador.unidades.dto.internal.response.KilometrajeRegistradoResponse;
import pe.edu.unc.elmirador.unidades.dto.response.ResultadoIdempotente;
import pe.edu.unc.elmirador.unidades.exceptions.KilometrajeRetrocedeException;
import pe.edu.unc.elmirador.unidades.services.UnidadInternalService;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(UnidadInternalController.class)
class UnidadInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UnidadInternalService servicio;

    @Test
    void elegibilidad_falsaEs200_jsonIgualAlContrato() throws Exception {
        ElegibilidadUnidadResponse respuesta = new ElegibilidadUnidadResponse(
                "UNI-004",
                false,
                List.of("DOCUMENTO_VENCIDO:SOAT", "MANTENIMIENTO_VENCIDO"),
                new ElegibilidadUnidadResponse.CapacidadDto(10000, new BigDecimal("32.0")),
                "FURGON",
                "INOPERATIVA"
        );
        
        when(servicio.elegibilidad(eq("UNI-004"), any(), any(), eq(10000), eq(new BigDecimal("32.0")), any()))
                .thenReturn(respuesta);

        mockMvc.perform(get("/internal/v1/unidades/UNI-004/elegibilidad")
                        .param("desde", "2026-09-10T06:00:00-05:00")
                        .param("hasta", "2026-09-10T18:00:00-05:00")
                        .param("pesoKg", "10000")
                        .param("volumenM3", "32.0")
                        .param("tipoCargaRequerido", "GENERAL"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "unidadId": "UNI-004",
                          "elegible": false,
                          "motivos": ["DOCUMENTO_VENCIDO:SOAT", "MANTENIMIENTO_VENCIDO"],
                          "capacidad": { "pesoMaximoKg": 10000, "volumenMaximoM3": 32.0 },
                          "tipoUnidad": "FURGON",
                          "estadoOperativo": "INOPERATIVA"
                        }
                        """));
    }

    @Test
    void reportarKilometraje_sinClave_400() throws Exception {
        ReportarKilometrajeRequest req = new ReportarKilometrajeRequest("VIA-01", 185000, OffsetDateTime.now(java.time.Clock.systemUTC()));
        mockMvc.perform(post("/internal/v1/unidades/UNI-004/kilometraje")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reportarKilometraje_conClave_200() throws Exception {
        ReportarKilometrajeRequest req = new ReportarKilometrajeRequest("VIA-01", 185000, OffsetDateTime.now(java.time.Clock.systemUTC()));
        when(servicio.reportarKilometraje(eq("UNI-004"), eq("clave-1"), any()))
                .thenReturn(new ResultadoIdempotente<>(new KilometrajeRegistradoResponse("UNI-004", "VIA-01"), false));

        mockMvc.perform(post("/internal/v1/unidades/UNI-004/kilometraje")
                        .header("Idempotency-Key", "clave-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unidadId").value("UNI-004"));
    }

    @Test
    void reportarKilometraje_violaUNI03_409() throws Exception {
        ReportarKilometrajeRequest req = new ReportarKilometrajeRequest("VIA-01", 1000, OffsetDateTime.now(java.time.Clock.systemUTC()));
        when(servicio.reportarKilometraje(eq("UNI-004"), eq("clave-2"), any()))
                .thenThrow(new KilometrajeRetrocedeException("No puede retroceder"));

        mockMvc.perform(post("/internal/v1/unidades/UNI-004/kilometraje")
                        .header("Idempotency-Key", "clave-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    void reportarFalla_conClave_200() throws Exception {
        ReportarFallaRequest req = new ReportarFallaRequest("VIA-01", "MECANICA", "Rotura", OffsetDateTime.now(java.time.Clock.systemUTC()), true);
        when(servicio.reportarFalla(eq("UNI-004"), eq("clave-3"), any()))
                .thenReturn(new ResultadoIdempotente<>(new FallaRegistradaResponse("UNI-004", "VIA-01"), false));

        mockMvc.perform(post("/internal/v1/unidades/UNI-004/fallas")
                        .header("Idempotency-Key", "clave-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unidadId").value("UNI-004"));
    }
}
