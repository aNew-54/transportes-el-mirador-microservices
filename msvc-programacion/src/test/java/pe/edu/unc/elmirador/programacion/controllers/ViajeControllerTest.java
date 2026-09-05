package pe.edu.unc.elmirador.programacion.controllers;

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
import java.time.OffsetDateTime;
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
import pe.edu.unc.elmirador.programacion.dto.request.AsignarRecursosRequest;
import pe.edu.unc.elmirador.programacion.dto.request.CapacidadRequest;
import pe.edu.unc.elmirador.programacion.dto.request.CargaRequest;
import pe.edu.unc.elmirador.programacion.dto.request.ClausulaDeConsolidacionRequest;
import pe.edu.unc.elmirador.programacion.dto.request.ConsolidarOrdenRequest;
import pe.edu.unc.elmirador.programacion.dto.request.ElegibilidadDeRecursoRequest;
import pe.edu.unc.elmirador.programacion.dto.request.ParadaRequest;
import pe.edu.unc.elmirador.programacion.dto.request.UbicacionRequest;
import pe.edu.unc.elmirador.programacion.dto.request.PlanificarViajeRequest;
import pe.edu.unc.elmirador.programacion.dto.request.ProgramarViajeRequest;
import pe.edu.unc.elmirador.programacion.dto.request.RutaRequest;
import pe.edu.unc.elmirador.programacion.dto.request.VentanaDeTiempoRequest;
import pe.edu.unc.elmirador.programacion.dto.response.CargaConsolidadaResponse;
import pe.edu.unc.elmirador.programacion.dto.response.CargaResponse;
import pe.edu.unc.elmirador.programacion.dto.response.RutaResponse;
import pe.edu.unc.elmirador.programacion.dto.response.VentanaDeTiempoResponse;
import pe.edu.unc.elmirador.programacion.dto.response.ViajeResponse;
import pe.edu.unc.elmirador.programacion.exceptions.AsignacionIncompletaException;
import pe.edu.unc.elmirador.programacion.exceptions.CapacidadExcedidaException;
import pe.edu.unc.elmirador.programacion.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.programacion.exceptions.DominioProgramacionException;
import pe.edu.unc.elmirador.programacion.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.programacion.exceptions.RecursoNoElegibleException;
import pe.edu.unc.elmirador.programacion.exceptions.TransicionDeViajeInvalidaException;
import pe.edu.unc.elmirador.programacion.models.vo.EstadoDeViaje;
import pe.edu.unc.elmirador.programacion.models.vo.TipoDeCarga;
import pe.edu.unc.elmirador.programacion.services.ViajeService;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(ViajeController.class)
class ViajeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @MockitoBean
    private ViajeService servicio;

    private ViajeResponse viajePlanificado() {
        return new ViajeResponse(
                "v-1",
                new RutaResponse("Lima", "Arequipa", "Sur"),
                new VentanaDeTiempoResponse(OffsetDateTime.parse("2026-03-10T10:00:00Z"), OffsetDateTime.parse("2026-03-11T10:00:00Z")),
                new CargaConsolidadaResponse(List.of(
                        new CargaResponse("ord-1", 1000, new BigDecimal("2.5"), TipoDeCarga.PALETIZADA, 1)
                ), 1000, new BigDecimal("2.5")),
                null,
                EstadoDeViaje.PLANIFICADO,
                null,
                List.of("ord-1")
        );
    }

    private PlanificarViajeRequest peticionValida() {
        return new PlanificarViajeRequest(
                "v-1",
                new RutaRequest("Lima", "Arequipa", "Sur"),
                new VentanaDeTiempoRequest(OffsetDateTime.parse("2026-03-10T10:00:00Z"), OffsetDateTime.parse("2026-03-11T10:00:00Z")),
                new CargaRequest("ord-1", 1000, new BigDecimal("2.5"), TipoDeCarga.PALETIZADA, 1)
        );
    }

    @Test
    @DisplayName("POST /viajes devuelve 201 con Location")
    void planificar201() throws Exception {
        when(servicio.planificar(any())).thenReturn(viajePlanificado());

        mockMvc.perform(post("/api/v1/viajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(peticionValida())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/viajes/v-1"))
                .andExpect(jsonPath("$.id").value("v-1"));
    }
    
    @Test
    @DisplayName("POST /viajes 404")
    void planificar404() throws Exception {
        when(servicio.planificar(any())).thenThrow(new RecursoNoEncontradoException("Orden", "ord-1"));

        mockMvc.perform(post("/api/v1/viajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(peticionValida())))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/recurso-no-encontrado"))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("No existe Orden con id ord-1"));
    }

    @Test
    @DisplayName("POST /viajes con id repetido devuelve 409")
    void planificar409() throws Exception {
        when(servicio.planificar(any())).thenThrow(new ConflictoDeRecursoException("Ya existe un viaje con id v-1"));

        mockMvc.perform(post("/api/v1/viajes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(peticionValida())))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/conflicto-de-recurso"))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Ya existe un viaje con id v-1"));
    }

    @Test
    @DisplayName("POST /viajes/{id}/ordenes consolida devuelve 200")
    void consolidar200() throws Exception {
        when(servicio.consolidarOrden(eq("v-1"), any())).thenReturn(viajePlanificado());

        ConsolidarOrdenRequest req = new ConsolidarOrdenRequest("ord-2", 2, new CapacidadRequest(20000, new BigDecimal("60.0")));

        mockMvc.perform(post("/api/v1/viajes/v-1/ordenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /viajes/{id}/ordenes excede capacidad devuelve 422")
    void consolidar422Capacidad() throws Exception {
        when(servicio.consolidarOrden(eq("v-1"), any())).thenThrow(new CapacidadExcedidaException("VIA-02: capacidad excedida"));

        ConsolidarOrdenRequest req = new ConsolidarOrdenRequest("ord-2", 2, new CapacidadRequest(20000, new BigDecimal("60.0")));

        mockMvc.perform(post("/api/v1/viajes/v-1/ordenes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/capacidad-excedida"))
                .andExpect(jsonPath("$.title").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").value("VIA-02: capacidad excedida"));
    }

    @Test
    @DisplayName("POST /viajes/{id}/recursos devuelve 200")
    void recursos200() throws Exception {
        when(servicio.asignarRecursos(eq("v-1"), any())).thenReturn(viajePlanificado());

        AsignarRecursosRequest req = new AsignarRecursosRequest("u-1", List.of("c-1"), false);

        mockMvc.perform(post("/api/v1/viajes/v-1/recursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /viajes/{id}/recursos con elegibilidad false devuelve 409")
    void recursos409Elegibilidad() throws Exception {
        when(servicio.asignarRecursos(eq("v-1"), any())).thenThrow(new RecursoNoElegibleException(List.of("motivo")));

        AsignarRecursosRequest req = new AsignarRecursosRequest("u-1", List.of("c-1"), false);

        mockMvc.perform(post("/api/v1/viajes/v-1/recursos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/recurso-no-elegible"))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    @DisplayName("POST /viajes/{id}/programar 200")
    void programar200() throws Exception {
        when(servicio.programar(eq("v-1"), any())).thenReturn(viajePlanificado());

        ProgramarViajeRequest req = new ProgramarViajeRequest(
                List.of(new ParadaRequest(1, "CARGA", "ord-1",
                        new UbicacionRequest("Jr. Ayacucho 450", "Cajamarca", "Almacen 2", "+51 976 000 111"),
                        OffsetDateTime.now())),
                "Coordinar con almacen del cliente antes de las 07:00."
        );

        mockMvc.perform(post("/api/v1/viajes/v-1/programar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /viajes/{id}/programar sin recursos completos devuelve 409")
    void programar409Incompleta() throws Exception {
        when(servicio.programar(eq("v-1"), any())).thenThrow(new AsignacionIncompletaException("VIA-01"));

        ProgramarViajeRequest req = new ProgramarViajeRequest(
                List.of(new ParadaRequest(1, "CARGA", "ord-1",
                        new UbicacionRequest("Jr. Ayacucho 450", "Cajamarca", "Almacen 2", "+51 976 000 111"),
                        OffsetDateTime.now())),
                "Coordinar con almacen del cliente antes de las 07:00."
        );

        mockMvc.perform(post("/api/v1/viajes/v-1/programar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/asignacion-incompleta"))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("VIA-01"));
    }

    @Test
    @DisplayName("POST /viajes/{id}/despachar 200")
    void despachar200() throws Exception {
        when(servicio.despachar(eq("v-1"))).thenReturn(viajePlanificado());

        mockMvc.perform(post("/api/v1/viajes/v-1/despachar"))
                .andExpect(status().isOk());
    }
    
    @Test
    @DisplayName("POST /viajes/{id}/despachar sin estado correcto devuelve 409")
    void despachar409() throws Exception {
        when(servicio.despachar(eq("v-1"))).thenThrow(new TransicionDeViajeInvalidaException("No se puede"));

        mockMvc.perform(post("/api/v1/viajes/v-1/despachar"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/transicion-invalida"))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("No se puede"));
    }

    @Test
    @DisplayName("POST /viajes/{id}/cancelar 200")
    void cancelar200() throws Exception {
        when(servicio.cancelar(eq("v-1"))).thenReturn(viajePlanificado());

        mockMvc.perform(post("/api/v1/viajes/v-1/cancelar"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /viajes/{id} devuelve 200")
    void porId200() throws Exception {
        when(servicio.consultar("v-1")).thenReturn(viajePlanificado());

        mockMvc.perform(get("/api/v1/viajes/v-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("v-1"));
    }

    @Test
    @DisplayName("GET /viajes/{id} inexistente devuelve 404")
    void porId404() throws Exception {
        when(servicio.consultar("no-existe")).thenThrow(new RecursoNoEncontradoException("Viaje", "no-existe"));

        mockMvc.perform(get("/api/v1/viajes/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/recurso-no-encontrado"))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("No existe Viaje con id no-existe"));
    }

    @Test
    @DisplayName("Comodin 422 manejador de errores")
    void comodin422() {
        ProblemDetail problema = new ManejadorDeErrores()
                .invarianteViolada(new DominioProgramacionException("Otra invariante rota"));

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(problema.getType())
                .hasToString("https://elmirador.unc.edu.pe/problems/invariante-violada");
    }
}
