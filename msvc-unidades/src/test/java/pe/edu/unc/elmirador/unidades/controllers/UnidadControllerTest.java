package pe.edu.unc.elmirador.unidades.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.edu.unc.elmirador.unidades.dto.request.CambiarEstadoRequest;
import pe.edu.unc.elmirador.unidades.dto.request.RegistrarUnidadRequest;
import pe.edu.unc.elmirador.unidades.dto.response.UnidadResponse;
import pe.edu.unc.elmirador.unidades.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.unidades.exceptions.KilometrajeRetrocedeException;
import pe.edu.unc.elmirador.unidades.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.unidades.models.vo.IntervaloDeMantenimiento;
import pe.edu.unc.elmirador.unidades.models.vo.SituacionOperativa;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeUnidad;
import pe.edu.unc.elmirador.unidades.services.UnidadService;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(UnidadController.class)
class UnidadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @MockitoBean
    private UnidadService servicio;

    private UnidadResponse respuestaDeEjemplo() {
        return new UnidadResponse(
                "u-1", "ABC-123", TipoDeUnidad.FURGON, 10000, new BigDecimal("30.00"),
                50000, SituacionOperativa.OPERATIVA, null, 40000, 50000,
                IntervaloDeMantenimiento.ACEITE_Y_FILTROS, List.of()
        );
    }

    private RegistrarUnidadRequest peticionValida() {
        return new RegistrarUnidadRequest(
                "ABC-123", TipoDeUnidad.FURGON, 10000, new BigDecimal("30.00"), 50000, IntervaloDeMantenimiento.ACEITE_Y_FILTROS
        );
    }

    @Test
    @DisplayName("POST /unidades devuelve 201 con Location")
    void registrar201() throws Exception {
        when(servicio.registrar(any())).thenReturn(respuestaDeEjemplo());

        mockMvc.perform(post("/api/v1/unidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(peticionValida())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/unidades/u-1"))
                .andExpect(jsonPath("$.id").value("u-1"));
    }

    @Test
    @DisplayName("POST /unidades con placa repetida devuelve 409")
    void registrar409() throws Exception {
        when(servicio.registrar(any()))
                .thenThrow(new ConflictoDeRecursoException("Ya existe una unidad con la placa ABC-123"));

        mockMvc.perform(post("/api/v1/unidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(peticionValida())))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/conflicto-de-recurso"));
    }

    @Test
    @DisplayName("GET /unidades/{id} devuelve 200")
    void porId200() throws Exception {
        when(servicio.porId("u-1")).thenReturn(respuestaDeEjemplo());

        mockMvc.perform(get("/api/v1/unidades/u-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placa").value("ABC-123"));
    }

    @Test
    @DisplayName("GET /unidades/{id} inexistente devuelve 404")
    void porId404() throws Exception {
        when(servicio.porId("no-existe")).thenThrow(new RecursoNoEncontradoException("Unidad", "no-existe"));

        mockMvc.perform(get("/api/v1/unidades/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/recurso-no-encontrado"));
    }

    @Test
    @DisplayName("GET /unidades devuelve lista")
    void listar200() throws Exception {
        when(servicio.listar(null)).thenReturn(List.of(respuestaDeEjemplo()));

        mockMvc.perform(get("/api/v1/unidades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("POST /unidades/{id}/documentos devuelve 201")
    void registrarDocumento201() throws Exception {
        when(servicio.registrarDocumento(eq("u-1"), any())).thenReturn(respuestaDeEjemplo());

        String cuerpo = """
                {"tipoDocumento":"SOAT","desde":"2026-01-01","hasta":"2027-01-01","numero":"123"}
                """;

        mockMvc.perform(post("/api/v1/unidades/u-1/documentos")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/unidades/u-1/documentos"));
    }

    @Test
    @DisplayName("POST /unidades/{id}/documentos sin cuerpo valido devuelve 400")
    void registrarDocumento400() throws Exception {
        mockMvc.perform(post("/api/v1/unidades/u-1/documentos")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores").exists());
    }

    @Test
    @DisplayName("POST /unidades/{id}/estado cambia el estado a INOPERATIVA")
    void cambiarEstado200() throws Exception {
        when(servicio.cambiarEstado(eq("u-1"), any())).thenReturn(respuestaDeEjemplo());

        mockMvc.perform(post("/api/v1/unidades/u-1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new CambiarEstadoRequest(SituacionOperativa.INOPERATIVA, "Falla de motor"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /unidades/{id}/estado lanza ReactivacionInvalidaException y devuelve 409")
    void cambiarEstado409() throws Exception {
        when(servicio.cambiarEstado(eq("u-1"), any()))
                .thenThrow(new pe.edu.unc.elmirador.unidades.exceptions.ReactivacionInvalidaException("No se puede reactivar"));

        mockMvc.perform(post("/api/v1/unidades/u-1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new CambiarEstadoRequest(SituacionOperativa.OPERATIVA, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/reactivacion-invalida"));
    }

    @Test
    @DisplayName("una invariante rota que nadie declaro sigue siendo 422")
    void comodinEs422() {
        ProblemDetail problema = new ManejadorDeErrores()
                .invarianteViolada(new KilometrajeRetrocedeException("No se puede retroceder"));

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(problema.getType()).hasToString("https://elmirador.unc.edu.pe/problems/invariante-violada");
    }
}
