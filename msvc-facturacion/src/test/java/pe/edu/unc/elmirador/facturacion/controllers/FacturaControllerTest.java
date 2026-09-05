package pe.edu.unc.elmirador.facturacion.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.edu.unc.elmirador.facturacion.dto.response.FacturaResponse;
import pe.edu.unc.elmirador.facturacion.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.facturacion.exceptions.EmisionSinConformidadException;
import pe.edu.unc.elmirador.facturacion.exceptions.ImportesInconsistentesException;
import pe.edu.unc.elmirador.facturacion.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.facturacion.services.FacturaService;
import java.util.List;

@WebMvcTest(FacturaController.class)
class FacturaControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private FacturaService servicio;

    @Test
    @DisplayName("POST /facturas devuelve 201")
    void abrir201() throws Exception {
        FacturaResponse resp = new FacturaResponse("f-1", "ord-1", "cli-1", null, null, null, null, null, null, false, null, null, null, null, null, null, List.of(), List.of());
        when(servicio.abrir(any())).thenReturn(resp);
        String json = """
            {
              "ordenDeServicioId": "ord-1",
              "clienteId": "cli-1",
              "snapshot": { "tarifaMonto": 100, "codigoMoneda": "PEN", "obtenidoEn": "2026-03-10T10:00:00Z" },
              "detraccion": { "porcentaje": 0, "monto": 0 }
            }
            """;
        mockMvc.perform(post("/api/v1/facturas").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/facturas/f-1"));
    }

    @Test
    @DisplayName("POST /facturas con orden repetida devuelve 409")
    void abrir409() throws Exception {
        when(servicio.abrir(any())).thenThrow(new ConflictoDeRecursoException("Ya existe"));
        String json = """
            {
              "ordenDeServicioId": "ord-1",
              "clienteId": "cli-1",
              "snapshot": { "tarifaMonto": 100, "codigoMoneda": "PEN", "obtenidoEn": "2026-03-10T10:00:00Z" },
              "detraccion": { "porcentaje": 0, "monto": 0 }
            }
            """;
        mockMvc.perform(post("/api/v1/facturas").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/conflicto-de-recurso"));
    }

    @Test
    @DisplayName("POST /facturas/{id}/emitir devuelve 200")
    void emitir200() throws Exception {
        FacturaResponse resp = new FacturaResponse("f-1", "ord-1", "cli-1", null, null, null, null, null, null, false, null, null, null, null, null, null, List.of(), List.of());
        when(servicio.emitir(eq("f-1"), any())).thenReturn(resp);
        String json = """
            { "serie": "F001", "correlativo": 1 }
            """;
        mockMvc.perform(post("/api/v1/facturas/f-1/emitir").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /facturas/{id}/emitir sin conformidad devuelve 409")
    void emitir409() throws Exception {
        when(servicio.emitir(eq("f-1"), any())).thenThrow(new EmisionSinConformidadException("Sin conformidad"));
        String json = """
            { "serie": "F001", "correlativo": 1 }
            """;
        mockMvc.perform(post("/api/v1/facturas/f-1/emitir").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/emision-sin-conformidad"));
    }

    @Test
    @DisplayName("POST /facturas/{id}/emitir con importes inconsistentes devuelve 422")
    void emitir422() throws Exception {
        when(servicio.emitir(eq("f-1"), any())).thenThrow(new ImportesInconsistentesException("Inconsistente"));
        String json = """
            { "serie": "F001", "correlativo": 1 }
            """;
        mockMvc.perform(post("/api/v1/facturas/f-1/emitir").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/importes-inconsistentes"));
    }

    @Test
    @DisplayName("GET /facturas/{id} devuelve 200")
    void get200() throws Exception {
        FacturaResponse resp = new FacturaResponse("f-1", "ord-1", "cli-1", null, null, null, null, null, null, false, null, null, null, null, null, null, List.of(), List.of());
        when(servicio.porId("f-1")).thenReturn(resp);
        mockMvc.perform(get("/api/v1/facturas/f-1"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /facturas/{id} devuelve 404")
    void get404() throws Exception {
        when(servicio.porId("f-2")).thenThrow(new RecursoNoEncontradoException("factura", "f-2"));
        mockMvc.perform(get("/api/v1/facturas/f-2"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /internal/v1/conformidades devuelve 200")
    void registrarConformidad200() throws Exception {
        String json = """
            {
              "ordenDeServicioId": "ord-1",
              "registrada": true,
              "incidenciasSinResolver": [],
              "recibidaEn": "2026-03-10T10:00:00Z",
              "conceptos": [
                 { "concepto": "FLETE", "descripcion": "Viaje", "importeMonto": 100 }
              ]
            }
            """;
        mockMvc.perform(post("/internal/v1/conformidades").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk());
    }
}
