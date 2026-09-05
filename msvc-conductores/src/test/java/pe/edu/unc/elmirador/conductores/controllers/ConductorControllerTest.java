package pe.edu.unc.elmirador.conductores.controllers;

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
import java.time.LocalDate;
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

import tools.jackson.databind.ObjectMapper;

import pe.edu.unc.elmirador.conductores.dto.request.RegistrarConductorRequest;
import pe.edu.unc.elmirador.conductores.dto.request.RegistrarInduccionRequest;
import pe.edu.unc.elmirador.conductores.dto.request.SuspenderConductorRequest;
import pe.edu.unc.elmirador.conductores.dto.response.ConductorResponse;
import pe.edu.unc.elmirador.conductores.dto.response.HorasResponse;
import pe.edu.unc.elmirador.conductores.dto.response.InduccionResponse;
import pe.edu.unc.elmirador.conductores.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.conductores.exceptions.HorasExcedidasException;
import pe.edu.unc.elmirador.conductores.exceptions.NumeroDeLicenciaInvalidoException;
import pe.edu.unc.elmirador.conductores.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.conductores.exceptions.RehabilitacionInvalidaException;
import pe.edu.unc.elmirador.conductores.models.vo.CategoriaDeLicencia;
import pe.edu.unc.elmirador.conductores.models.vo.SituacionDeHabilitacion;
import pe.edu.unc.elmirador.conductores.services.ConductorService;

/**
 * Una prueba por fila de la tabla de la API publica de la spec, mas el mapa de codigos.
 *
 * <p>El servicio va sustituido: aqui no se comprueba ninguna regla de negocio, solo la ruta, el
 * verbo, el codigo y la forma del cuerpo.
 */
@WebMvcTest(ConductorController.class)
class ConductorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @MockitoBean
    private ConductorService servicio;

    private ConductorResponse respuestaDeEjemplo() {
        return new ConductorResponse(
                "c-1", "Juan Perez Vasquez", "Q12345678", CategoriaDeLicencia.A_IIIB,
                LocalDate.of(2025, 1, 1), LocalDate.of(2027, 1, 1),
                SituacionDeHabilitacion.HABILITADO, null,
                new HorasResponse("c-1", new BigDecimal("0.00"), new BigDecimal("10.00"),
                        new BigDecimal("10.00"), LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 11)),
                List.of());
    }

    private RegistrarConductorRequest peticionValida() {
        return new RegistrarConductorRequest(
                "Juan Perez Vasquez", "Q12345678", CategoriaDeLicencia.A_IIIB,
                LocalDate.of(2025, 1, 1), LocalDate.of(2027, 1, 1));
    }

    // ---------- POST /conductores : 201 400 409 ----------

    @Test
    @DisplayName("POST /conductores devuelve 201 con Location")
    void registrar201() throws Exception {
        when(servicio.registrar(any())).thenReturn(respuestaDeEjemplo());

        mockMvc.perform(post("/api/v1/conductores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(peticionValida())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/conductores/c-1"))
                .andExpect(jsonPath("$.id").value("c-1"))
                .andExpect(jsonPath("$.numeroDeLicencia").value("Q12345678"));
    }

    @Test
    @DisplayName("POST /conductores sin nombre devuelve 400 con problem+json y el detalle por campo")
    void registrar400PorValidacion() throws Exception {
        String cuerpo = """
                {"nombreCompleto":"  ","numeroDeLicencia":"Q12345678",
                 "categoriaDeLicencia":"A_IIIB","licenciaDesde":"2025-01-01","licenciaHasta":"2027-01-01"}
                """;

        mockMvc.perform(post("/api/v1/conductores")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/validacion"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errores.nombreCompleto").exists());
    }

    @Test
    @DisplayName("POST /conductores con una licencia mal formada no llega al servicio: 400")
    void registrar400PorFormatoDeLicencia() throws Exception {
        String cuerpo = """
                {"nombreCompleto":"Juan Perez","numeroDeLicencia":"12345678",
                 "categoriaDeLicencia":"A_IIIB","licenciaDesde":"2025-01-01","licenciaHasta":"2027-01-01"}
                """;

        mockMvc.perform(post("/api/v1/conductores")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.numeroDeLicencia").exists());
    }

    @Test
    @DisplayName("POST /conductores con licencia repetida devuelve 409")
    void registrar409() throws Exception {
        when(servicio.registrar(any()))
                .thenThrow(new ConflictoDeRecursoException("Ya existe un conductor con la licencia Q12345678"));

        mockMvc.perform(post("/api/v1/conductores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(peticionValida())))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/conflicto-de-recurso"));
    }

    // ---------- GET /conductores/{id} : 200 404 ----------

    @Test
    @DisplayName("GET /conductores/{id} devuelve 200")
    void porId200() throws Exception {
        when(servicio.porId("c-1")).thenReturn(respuestaDeEjemplo());

        mockMvc.perform(get("/api/v1/conductores/c-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto").value("Juan Perez Vasquez"))
                .andExpect(jsonPath("$.horas.maximoNormado").value(10.00));
    }

    @Test
    @DisplayName("GET /conductores/{id} inexistente devuelve 404 con problem+json")
    void porId404() throws Exception {
        when(servicio.porId("no-existe"))
                .thenThrow(new RecursoNoEncontradoException("conductor", "no-existe"));

        mockMvc.perform(get("/api/v1/conductores/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/recurso-no-encontrado"))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("No existe conductor con id no-existe"));
    }

    // ---------- GET /conductores : 200 ----------

    @Test
    @DisplayName("GET /conductores pasa el filtro de situacion al servicio")
    void listar200() throws Exception {
        when(servicio.listar(SituacionDeHabilitacion.SUSPENDIDO)).thenReturn(List.of());
        when(servicio.listar(null)).thenReturn(List.of(respuestaDeEjemplo()));

        mockMvc.perform(get("/api/v1/conductores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/v1/conductores").param("situacion", "SUSPENDIDO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---------- POST /conductores/{id}/licencia : 200 400 404 ----------

    @Test
    @DisplayName("POST /conductores/{id}/licencia devuelve 200")
    void renovarLicencia200() throws Exception {
        when(servicio.renovarLicencia(eq("c-1"), any())).thenReturn(respuestaDeEjemplo());

        String cuerpo = """
                {"numeroDeLicencia":"Q12345678","categoriaDeLicencia":"A_IIIC",
                 "vigenteDesde":"2026-01-01","vigenteHasta":"2031-01-01"}
                """;

        mockMvc.perform(post("/api/v1/conductores/c-1/licencia")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("el objeto de valor que rechaza el formato tambien se traduce a 400")
    void renovarLicencia400DesdeElDominio() throws Exception {
        when(servicio.renovarLicencia(eq("c-1"), any()))
                .thenThrow(new NumeroDeLicenciaInvalidoException("El formato del numero de licencia es invalido: Q1"));

        String cuerpo = """
                {"numeroDeLicencia":"Q12345678","categoriaDeLicencia":"A_IIIC",
                 "vigenteDesde":"2026-01-01","vigenteHasta":"2031-01-01"}
                """;

        mockMvc.perform(post("/api/v1/conductores/c-1/licencia")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type")
                        .value("https://elmirador.unc.edu.pe/problems/numero-de-licencia-invalido"));
    }

    // ---------- POST /conductores/{id}/inducciones : 201 400 404 ----------

    @Test
    @DisplayName("POST /conductores/{id}/inducciones devuelve 201 con Location")
    void registrarInduccion201() throws Exception {
        when(servicio.registrarInduccion(eq("c-1"), any())).thenReturn(
                new InduccionResponse("i-7", "cli-9", LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1)));

        mockMvc.perform(post("/api/v1/conductores/c-1/inducciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RegistrarInduccionRequest(
                                "cli-9", LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1)))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/conductores/c-1/inducciones/i-7"))
                .andExpect(jsonPath("$.clienteId").value("cli-9"));
    }

    @Test
    @DisplayName("POST /conductores/{id}/inducciones sobre un conductor inexistente devuelve 404")
    void registrarInduccion404() throws Exception {
        when(servicio.registrarInduccion(eq("no-existe"), any()))
                .thenThrow(new RecursoNoEncontradoException("conductor", "no-existe"));

        mockMvc.perform(post("/api/v1/conductores/no-existe/inducciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RegistrarInduccionRequest(
                                "cli-9", LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1)))))
                .andExpect(status().isNotFound());
    }

    // ---------- GET /conductores/{id}/horas : 200 404 ----------

    @Test
    @DisplayName("GET /conductores/{id}/horas devuelve el acumulado y lo disponible")
    void horas200() throws Exception {
        when(servicio.horas("c-1")).thenReturn(new HorasResponse(
                "c-1", new BigDecimal("6.50"), new BigDecimal("3.50"), new BigDecimal("10.00"),
                LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 11)));

        mockMvc.perform(get("/api/v1/conductores/c-1/horas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acumuladas").value(6.50))
                .andExpect(jsonPath("$.disponibles").value(3.50));
    }

    // ---------- POST /conductores/{id}/descanso : 200 404 ----------

    @Test
    @DisplayName("POST /conductores/{id}/descanso no necesita cuerpo: la fecha la pone el reloj")
    void descanso200() throws Exception {
        when(servicio.registrarDescanso("c-1")).thenReturn(respuestaDeEjemplo());

        mockMvc.perform(post("/api/v1/conductores/c-1/descanso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horas.acumuladas").value(0.00));
    }

    // ---------- POST /conductores/{id}/suspender : 200 400 404 ----------

    @Test
    @DisplayName("POST /conductores/{id}/suspender sin motivo devuelve 400")
    void suspender400SinMotivo() throws Exception {
        mockMvc.perform(post("/api/v1/conductores/c-1/suspender")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.motivo").exists());
    }

    @Test
    @DisplayName("POST /conductores/{id}/suspender devuelve 200 con el motivo registrado")
    void suspender200() throws Exception {
        when(servicio.suspender(eq("c-1"), any())).thenReturn(new ConductorResponse(
                "c-1", "Juan Perez Vasquez", "Q12345678", CategoriaDeLicencia.A_IIIB,
                LocalDate.of(2025, 1, 1), LocalDate.of(2027, 1, 1),
                SituacionDeHabilitacion.SUSPENDIDO, "papeleta pendiente",
                respuestaDeEjemplo().horas(), List.of()));

        mockMvc.perform(post("/api/v1/conductores/c-1/suspender")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new SuspenderConductorRequest("papeleta pendiente"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacion").value("SUSPENDIDO"))
                .andExpect(jsonPath("$.motivo").value("papeleta pendiente"));
    }

    // ---------- POST /conductores/{id}/rehabilitar : 200 404 409 ----------

    @Test
    @DisplayName("POST /conductores/{id}/rehabilitar con la licencia vencida devuelve 409, no 422")
    void rehabilitar409() throws Exception {
        when(servicio.rehabilitar("c-2"))
                .thenThrow(new RehabilitacionInvalidaException("No se puede rehabilitar: licencia no vigente"));

        mockMvc.perform(post("/api/v1/conductores/c-2/rehabilitar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type")
                        .value("https://elmirador.unc.edu.pe/problems/rehabilitacion-invalida"));
    }

    // ---------- GET /alertas : 200 ----------

    @Test
    @DisplayName("GET /alertas usa 30 dias por defecto")
    void alertas200() throws Exception {
        when(servicio.alertas(30)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/alertas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---------- El mapa de codigos ----------

    /**
     * El comodin del manejador: cualquier {@code DominioConductoresException} que nadie liste arriba
     * es {@code 422}, nunca {@code 500}. Se prueba contra el manejador directamente porque ningun
     * endpoint de {@code S3} lanza CON-02: las horas entran por el contrato 6, que es de {@code S4}.
     */
    @Test
    @DisplayName("una invariante rota que nadie declaro sigue siendo 422 y no 500")
    void comodinEs422() {
        ProblemDetail problema = new ManejadorDeErrores()
                .invarianteViolada(new HorasExcedidasException("Las horas acumuladas superan el maximo"));

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(problema.getType())
                .hasToString("https://elmirador.unc.edu.pe/problems/invariante-violada");
        assertThat(problema.getDetail()).isEqualTo("Las horas acumuladas superan el maximo");
    }
}
