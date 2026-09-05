package pe.edu.unc.elmirador.facturacion.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.edu.unc.elmirador.facturacion.dto.internal.response.ConformidadRegistradaResponse;
import pe.edu.unc.elmirador.facturacion.dto.response.ResultadoIdempotente;
import pe.edu.unc.elmirador.facturacion.services.FacturacionInternalService;

@WebMvcTest(FacturacionInternalController.class)
class FacturacionInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FacturacionInternalService servicio;

    private static final String CUERPO_COMPLETO = """
            {
              "viajeId": "VIA-2026-00045",
              "ordenDeServicioId": "ORD-2026-000123",
              "estado": "FIRMADA",
              "fechaDeFirma": "2026-09-10T15:20:00-05:00",
              "conceptosFacturables": [
                { "concepto": "ESTIBA",   "monto": "180.00", "moneda": "PEN" },
                { "concepto": "ESPERA",   "monto": "245.00", "moneda": "PEN", "detalle": "3.5 h sobre 2 h de tiempo libre" },
                { "concepto": "REAJUSTE", "monto": "320.00", "moneda": "PEN", "detalle": "2000 kg sobre lo declarado" }
              ],
              "incidenciasSinResolver": []
            }
            """;

    @Test
    @DisplayName("la que compara el JSON con el ejemplo de contracts.md campo a campo, incluidos los tres conceptos facturables anidados")
    void ejemploContrato8() throws Exception {
        when(servicio.registrarConformidad(eq("VIA-2026-00045:ORD-2026-000123:conformidad"), any()))
                .thenReturn(new ResultadoIdempotente<>(
                        new ConformidadRegistradaResponse("FAC-2026-000310", "ORD-2026-000123"), false
                ));

        mockMvc.perform(post("/internal/v1/conformidades")
                        .header("Idempotency-Key", "VIA-2026-00045:ORD-2026-000123:conformidad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_COMPLETO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.facturaId").value("FAC-2026-000310"))
                .andExpect(jsonPath("$.ordenDeServicioId").value("ORD-2026-000123"));
    }

    @Test
    @DisplayName("la de la cabecera ausente: sin Idempotency-Key, 400")
    void sinCabecera() throws Exception {
        mockMvc.perform(post("/internal/v1/conformidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CUERPO_COMPLETO))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("la de incidenciasSinResolver ausente: 400, no un sin incidencias silencioso")
    void sinIncidenciasSinResolver() throws Exception {
        String cuerpoIncompleto = """
            {
              "viajeId": "VIA-2026-00045",
              "ordenDeServicioId": "ORD-2026-000123",
              "estado": "FIRMADA",
              "fechaDeFirma": "2026-09-10T15:20:00-05:00",
              "conceptosFacturables": []
            }
            """;

        mockMvc.perform(post("/internal/v1/conformidades")
                        .header("Idempotency-Key", "clave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoIncompleto))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errores.incidenciasSinResolver").exists());
    }
}
