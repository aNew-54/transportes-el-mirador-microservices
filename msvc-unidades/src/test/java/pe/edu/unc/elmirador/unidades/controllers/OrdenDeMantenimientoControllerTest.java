package pe.edu.unc.elmirador.unidades.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.edu.unc.elmirador.unidades.dto.request.AbrirOrdenRequest;
import pe.edu.unc.elmirador.unidades.dto.response.OrdenDeMantenimientoResponse;
import pe.edu.unc.elmirador.unidades.exceptions.KilometrajeDeAtencionInvalidoException;
import pe.edu.unc.elmirador.unidades.models.vo.EstadoDeOrden;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeMantenimiento;
import pe.edu.unc.elmirador.unidades.services.OrdenDeMantenimientoService;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(OrdenDeMantenimientoController.class)
class OrdenDeMantenimientoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @MockitoBean
    private OrdenDeMantenimientoService servicio;

    private OrdenDeMantenimientoResponse respuestaDeEjemplo() {
        return new OrdenDeMantenimientoResponse(
                "o-1", "u-1", TipoDeMantenimiento.PREVENTIVO, 50000,
                EstadoDeOrden.ABIERTA, LocalDate.of(2026, 1, 1), null,
                BigDecimal.ZERO, "PEN", List.of()
        );
    }

    @Test
    @DisplayName("POST /ordenes-mantenimiento devuelve 201")
    void abrir201() throws Exception {
        when(servicio.abrir(any())).thenReturn(respuestaDeEjemplo());

        mockMvc.perform(post("/api/v1/ordenes-mantenimiento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new AbrirOrdenRequest("u-1", TipoDeMantenimiento.PREVENTIVO, 50000, "PEN"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/ordenes-mantenimiento/o-1"))
                .andExpect(jsonPath("$.id").value("o-1"));
    }

    @Test
    @DisplayName("POST /ordenes-mantenimiento con km invalido devuelve 422")
    void abrir422() throws Exception {
        when(servicio.abrir(any())).thenThrow(new KilometrajeDeAtencionInvalidoException("Invalido"));

        mockMvc.perform(post("/api/v1/ordenes-mantenimiento")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new AbrirOrdenRequest("u-1", TipoDeMantenimiento.PREVENTIVO, 10, "PEN"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/invariante-violada"));
    }

    @Test
    @DisplayName("POST /ordenes-mantenimiento/{id}/cerrar devuelve 200")
    void cerrar200() throws Exception {
        when(servicio.cerrar("o-1")).thenReturn(respuestaDeEjemplo());

        mockMvc.perform(post("/api/v1/ordenes-mantenimiento/o-1/cerrar"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /ordenes-mantenimiento/{id}/trabajos devuelve 201")
    void registrarTrabajo201() throws Exception {
        when(servicio.registrarTrabajo(eq("o-1"), any())).thenReturn(respuestaDeEjemplo());

        String cuerpo = """
                {"descripcion":"Cambio de aceite","costoManoDeObra":150.00,"monedaManoDeObra":"PEN"}
                """;

        mockMvc.perform(post("/api/v1/ordenes-mantenimiento/o-1/trabajos")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/ordenes-mantenimiento/o-1/trabajos"));
    }

    @Test
    @DisplayName("POST /ordenes-mantenimiento/{id}/trabajos en orden cerrada devuelve 409")
    void registrarTrabajo409() throws Exception {
        when(servicio.registrarTrabajo(eq("o-1"), any()))
                .thenThrow(new pe.edu.unc.elmirador.unidades.exceptions.OrdenCerradaException("Orden cerrada"));

        String cuerpo = """
                {"descripcion":"Cambio de aceite","costoManoDeObra":150.00,"monedaManoDeObra":"PEN"}
                """;

        mockMvc.perform(post("/api/v1/ordenes-mantenimiento/o-1/trabajos")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/orden-cerrada"));
    }

    @Test
    @DisplayName("POST /ordenes-mantenimiento/{id}/cerrar ya cerrada devuelve 409")
    void cerrar409() throws Exception {
        when(servicio.cerrar("o-1"))
                .thenThrow(new pe.edu.unc.elmirador.unidades.exceptions.OrdenCerradaException("Orden cerrada"));

        mockMvc.perform(post("/api/v1/ordenes-mantenimiento/o-1/cerrar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/orden-cerrada"));
    }
}
