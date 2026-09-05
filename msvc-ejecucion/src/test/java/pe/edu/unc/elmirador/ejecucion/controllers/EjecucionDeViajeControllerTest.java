package pe.edu.unc.elmirador.ejecucion.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import pe.edu.unc.elmirador.ejecucion.dto.request.CerrarEjecucionRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.HorasDeConductorRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.ConformidadRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.CrearEjecucionRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.ParadaRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.RegistrarCheckListRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.RegistrarIncidenciaRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.ReportarHitoRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.TransbordoRequest;
import pe.edu.unc.elmirador.ejecucion.dto.response.EjecucionDeViajeResponse;
import pe.edu.unc.elmirador.ejecucion.exceptions.CheckListNoAprobadoException;
import pe.edu.unc.elmirador.ejecucion.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.ejecucion.exceptions.ConformidadesPendientesException;
import pe.edu.unc.elmirador.ejecucion.exceptions.EjecucionEntregadaException;
import pe.edu.unc.elmirador.ejecucion.exceptions.EvidenciaRequeridaException;
import pe.edu.unc.elmirador.ejecucion.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.ejecucion.exceptions.TransicionDeEjecucionInvalidaException;
import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoConformidad;
import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoDeEjecucion;
import pe.edu.unc.elmirador.ejecucion.models.vo.TipoDeHito;
import pe.edu.unc.elmirador.ejecucion.models.vo.TipoDeIncidencia;
import pe.edu.unc.elmirador.ejecucion.services.EjecucionDeViajeService;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(EjecucionDeViajeController.class)
class EjecucionDeViajeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @MockitoBean
    private EjecucionDeViajeService servicio;

    private EjecucionDeViajeResponse respuestaDeEjemplo() {
        return new EjecucionDeViajeResponse(
                "v-1", "u-1", List.of("c-1"), null, EstadoDeEjecucion.PENDIENTE, null,
                List.of(), List.of(), List.of(), List.of());
    }

    @Test
    @DisplayName("POST /ejecuciones devuelve 201 con Location")
    void crear201() throws Exception {
        when(servicio.crear(any())).thenReturn(respuestaDeEjemplo());

        CrearEjecucionRequest peticion = new CrearEjecucionRequest("v-1");

        mockMvc.perform(post("/api/v1/ejecuciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(peticion)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/ejecuciones/v-1"))
                .andExpect(jsonPath("$.viajeId").value("v-1"));
    }

    @Test
    @DisplayName("POST /ejecuciones con viajeId repetido devuelve 409")
    void crear409() throws Exception {
        when(servicio.crear(any()))
                .thenThrow(new ConflictoDeRecursoException("Ya existe"));

        CrearEjecucionRequest peticion = new CrearEjecucionRequest("v-1");

        mockMvc.perform(post("/api/v1/ejecuciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(peticion)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/conflicto-de-recurso"));
    }

    @Test
    @DisplayName("POST /ejecuciones/{viajeId}/checklist devuelve 200")
    void registrarChecklist200() throws Exception {
        when(servicio.registrarCheckList(eq("v-1"), any())).thenReturn(respuestaDeEjemplo());

        RegistrarCheckListRequest peticion = new RegistrarCheckListRequest(true, List.of());

        mockMvc.perform(post("/api/v1/ejecuciones/v-1/checklist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(peticion)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /ejecuciones/{viajeId}/iniciar devuelve 200")
    void iniciar200() throws Exception {
        when(servicio.iniciar("v-1")).thenReturn(respuestaDeEjemplo());

        mockMvc.perform(post("/api/v1/ejecuciones/v-1/iniciar"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /ejecuciones/{viajeId}/iniciar sin checklist aprobado devuelve 409")
    void iniciar409() throws Exception {
        when(servicio.iniciar("v-1")).thenThrow(new CheckListNoAprobadoException("No aprobado"));

        mockMvc.perform(post("/api/v1/ejecuciones/v-1/iniciar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/checklist-no-aprobado"));
    }

    @Test
    @DisplayName("POST /ejecuciones/{viajeId}/hitos devuelve 201")
    void reportarHito201() throws Exception {
        when(servicio.reportarHito(eq("v-1"), any())).thenReturn(respuestaDeEjemplo());

        ReportarHitoRequest peticion = new ReportarHitoRequest(TipoDeHito.SALIDA, "Planta");

        mockMvc.perform(post("/api/v1/ejecuciones/v-1/hitos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(peticion)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /ejecuciones/{viajeId}/hitos en viaje entregado devuelve 409")
    void reportarHito409() throws Exception {
        when(servicio.reportarHito(eq("v-1"), any())).thenThrow(new EjecucionEntregadaException("Entregada"));

        ReportarHitoRequest peticion = new ReportarHitoRequest(TipoDeHito.SALIDA, "Planta");

        mockMvc.perform(post("/api/v1/ejecuciones/v-1/hitos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(peticion)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/ejecucion-entregada"));
    }

    @Test
    @DisplayName("POST /ejecuciones/{viajeId}/incidencias devuelve 201")
    void registrarIncidencia201() throws Exception {
        when(servicio.registrarIncidencia(eq("v-1"), any())).thenReturn(respuestaDeEjemplo());

        RegistrarIncidenciaRequest peticion = new RegistrarIncidenciaRequest(
                TipoDeIncidencia.DEMORA, "Trafico", List.of());

        mockMvc.perform(post("/api/v1/ejecuciones/v-1/incidencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(peticion)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /ejecuciones/{viajeId}/incidencias sin evidencia cuando es requerida devuelve 422")
    void registrarIncidencia422() throws Exception {
        when(servicio.registrarIncidencia(eq("v-1"), any()))
                .thenThrow(new EvidenciaRequeridaException("Se requiere evidencia"));

        RegistrarIncidenciaRequest peticion = new RegistrarIncidenciaRequest(
                TipoDeIncidencia.DANIO, "Choque", List.of());

        mockMvc.perform(post("/api/v1/ejecuciones/v-1/incidencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(peticion)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/evidencia-requerida"));
    }

    @Test
    @DisplayName("POST /ejecuciones/{viajeId}/transbordo devuelve 200")
    void transbordo200() throws Exception {
        when(servicio.transbordar(eq("v-1"), any())).thenReturn(respuestaDeEjemplo());

        TransbordoRequest peticion = new TransbordoRequest("u-2");

        mockMvc.perform(post("/api/v1/ejecuciones/v-1/transbordo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(peticion)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /ejecuciones/{viajeId}/paradas/{secuencia}/conformidad devuelve 201")
    void registrarConformidad201() throws Exception {
        when(servicio.registrarConformidad(eq("v-1"), eq(1), any())).thenReturn(respuestaDeEjemplo());

        ConformidadRequest peticion = new ConformidadRequest(EstadoConformidad.FIRMADA, "Juan", "");

        mockMvc.perform(post("/api/v1/ejecuciones/v-1/paradas/1/conformidad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(peticion)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /ejecuciones/{viajeId}/cerrar devuelve 200")
    void cerrar200() throws Exception {
        when(servicio.cerrar(eq("v-1"), any())).thenReturn(respuestaDeEjemplo());

        CerrarEjecucionRequest peticion = new CerrarEjecucionRequest(
                184320,
                List.of(new HorasDeConductorRequest("c-1", 8.5,
                        OffsetDateTime.parse("2026-09-10T06:00:00-05:00"),
                        OffsetDateTime.parse("2026-09-10T14:30:00-05:00"))),
                List.of());

        mockMvc.perform(post("/api/v1/ejecuciones/v-1/cerrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(peticion)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /ejecuciones/{viajeId} devuelve 200")
    void obtener200() throws Exception {
        when(servicio.obtener("v-1")).thenReturn(respuestaDeEjemplo());

        mockMvc.perform(get("/api/v1/ejecuciones/v-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /ejecuciones/{viajeId} inexistente devuelve 404")
    void obtener404() throws Exception {
        when(servicio.obtener("no-existe")).thenThrow(new RecursoNoEncontradoException("EjecucionDeViaje", "no-existe"));

        mockMvc.perform(get("/api/v1/ejecuciones/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/recurso-no-encontrado"));
    }
}
