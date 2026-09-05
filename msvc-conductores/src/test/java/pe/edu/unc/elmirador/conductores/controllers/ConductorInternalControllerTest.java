package pe.edu.unc.elmirador.conductores.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

import pe.edu.unc.elmirador.conductores.dto.internal.response.ElegibilidadResponse;
import pe.edu.unc.elmirador.conductores.dto.internal.response.HorasRegistradasResponse;
import pe.edu.unc.elmirador.conductores.dto.internal.response.IncidenciaRegistradaResponse;
import pe.edu.unc.elmirador.conductores.dto.response.ResultadoIdempotente;
import pe.edu.unc.elmirador.conductores.exceptions.HorasExcedidasException;
import pe.edu.unc.elmirador.conductores.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.conductores.models.vo.TipoDeUnidad;
import pe.edu.unc.elmirador.conductores.services.ConductorInternalService;

/**
 * Lado proveedor de los contratos 3 y 6. Una prueba por fila de la tabla de estados de
 * {@code docs/api/contracts.md}, mas la que compara el JSON con el ejemplo del contrato campo a campo.
 */
@WebMvcTest(ConductorInternalController.class)
class ConductorInternalControllerTest {

    private static final String DESDE = "2026-09-10T06:00:00-05:00";
    private static final String HASTA = "2026-09-10T14:30:00-05:00";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConductorInternalService servicio;

    // ---------- Contrato 3 ----------

    /**
     * El ejemplo literal del contrato 3. Es lo unico que demuestra que el proveedor cumple la forma
     * pactada: los codigos de estado se pueden acertar con la forma equivocada.
     */
    @Test
    @DisplayName("contrato 3: la respuesta tiene los cinco campos del ejemplo, con esos nombres")
    void formaDelContrato3() throws Exception {
        when(servicio.elegibilidad(eq("CON-011"), any(), any(), eq(TipoDeUnidad.FURGON), eq("CLI-0019")))
                .thenReturn(new ElegibilidadResponse(
                        "CON-011", false,
                        List.of("INDUCCION_VENCIDA:CLI-0019", "HORAS_INSUFICIENTES"),
                        "A-IIIB", new BigDecimal("3.5")));

        mockMvc.perform(get("/internal/v1/conductores/CON-011/elegibilidad")
                        .param("desde", DESDE).param("hasta", HASTA)
                        .param("tipoUnidad", "FURGON").param("clienteId", "CLI-0019"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conductorId").value("CON-011"))
                .andExpect(jsonPath("$.elegible").value(false))
                .andExpect(jsonPath("$.motivos[0]").value("INDUCCION_VENCIDA:CLI-0019"))
                .andExpect(jsonPath("$.motivos[1]").value("HORAS_INSUFICIENTES"))
                .andExpect(jsonPath("$.categoriaLicencia").value("A-IIIB"))
                .andExpect(jsonPath("$.horasDisponibles").value(3.5));
    }

    /** Regla 5: no elegible es una respuesta de negocio con {@code 200}, nunca un error. */
    @Test
    @DisplayName("contrato 3: elegible false sigue siendo 200")
    void noElegibleEs200() throws Exception {
        when(servicio.elegibilidad(any(), any(), any(), any(), isNull()))
                .thenReturn(new ElegibilidadResponse(
                        "CON-011", false, List.of("NO_HABILITADO"), "A-IIIA", new BigDecimal("10.00")));

        mockMvc.perform(get("/internal/v1/conductores/CON-011/elegibilidad")
                        .param("desde", DESDE).param("hasta", HASTA).param("tipoUnidad", "FURGON"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elegible").value(false));
    }

    @Test
    @DisplayName("contrato 3: un conductor inexistente es 404 con problem+json")
    void elegibilidad404() throws Exception {
        when(servicio.elegibilidad(eq("no-existe"), any(), any(), any(), isNull()))
                .thenThrow(new RecursoNoEncontradoException("conductor", "no-existe"));

        mockMvc.perform(get("/internal/v1/conductores/no-existe/elegibilidad")
                        .param("desde", DESDE).param("hasta", HASTA).param("tipoUnidad", "FURGON"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/recurso-no-encontrado"));
    }

    @Test
    @DisplayName("contrato 3: clienteId es opcional")
    void clienteIdOpcional() throws Exception {
        when(servicio.elegibilidad(any(), any(), any(), any(), isNull()))
                .thenReturn(new ElegibilidadResponse(
                        "CON-011", true, List.of(), "A-IIIC", new BigDecimal("10.00")));

        mockMvc.perform(get("/internal/v1/conductores/CON-011/elegibilidad")
                        .param("desde", DESDE).param("hasta", HASTA).param("tipoUnidad", "CAMA_BAJA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elegible").value(true));
    }

    // ---------- Contrato 6 ----------

    private static final String CUERPO_HORAS = """
            { "viajeId": "VIA-2026-00045", "horas": 8.5,
              "desde": "2026-09-10T06:00:00-05:00", "hasta": "2026-09-10T14:30:00-05:00" }
            """;

    @Test
    @DisplayName("contrato 6: registrar horas devuelve 200")
    void horas200() throws Exception {
        when(servicio.reportarHoras(eq("CON-011"), eq("VIA-2026-00045:CON-011:horas"), any()))
                .thenReturn(new ResultadoIdempotente<>(new HorasRegistradasResponse(
                        "CON-011", "VIA-2026-00045", new BigDecimal("8.50"), new BigDecimal("1.50"),
                        LocalDate.of(2026, 9, 10)), false));

        mockMvc.perform(post("/internal/v1/conductores/CON-011/horas-conduccion")
                        .header("Idempotency-Key", "VIA-2026-00045:CON-011:horas")
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO_HORAS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horasAcumuladas").value(8.50))
                .andExpect(jsonPath("$.horasDisponibles").value(1.50));
    }

    /** El reintento devuelve el mismo {@code 200} y el mismo cuerpo: lo dice la tabla del contrato. */
    @Test
    @DisplayName("contrato 6: el reintento con la misma clave tambien devuelve 200")
    void horasReintento200() throws Exception {
        when(servicio.reportarHoras(any(), any(), any()))
                .thenReturn(new ResultadoIdempotente<>(new HorasRegistradasResponse(
                        "CON-011", "VIA-2026-00045", new BigDecimal("8.50"), new BigDecimal("1.50"),
                        LocalDate.of(2026, 9, 10)), true));

        mockMvc.perform(post("/internal/v1/conductores/CON-011/horas-conduccion")
                        .header("Idempotency-Key", "VIA-2026-00045:CON-011:horas")
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO_HORAS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horasAcumuladas").value(8.50));
    }

    @Test
    @DisplayName("contrato 6: superar el maximo normado es 409, tal como pide la tabla")
    void horas409() throws Exception {
        when(servicio.reportarHoras(any(), any(), any()))
                .thenThrow(new HorasExcedidasException(
                        "Las horas acumuladas (11.00) superarian el maximo normado de 10.00"));

        mockMvc.perform(post("/internal/v1/conductores/CON-011/horas-conduccion")
                        .header("Idempotency-Key", "k-1")
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO_HORAS))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/horas-excedidas"));
    }

    /**
     * Sin la cabecera no se puede distinguir un reintento de red de un segundo reporte, y las horas
     * se sumarian dos veces. Es exactamente el fallo que la regla 6 pide evitar, asi que es un 400.
     */
    @Test
    @DisplayName("contrato 6: sin Idempotency-Key la peticion es 400")
    void sinCabeceraDeIdempotencia() throws Exception {
        mockMvc.perform(post("/internal/v1/conductores/CON-011/horas-conduccion")
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO_HORAS))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("contrato 6: registrar una incidencia devuelve 200")
    void incidencia200() throws Exception {
        when(servicio.reportarIncidencia(eq("CON-011"), any(), any()))
                .thenReturn(new ResultadoIdempotente<>(new IncidenciaRegistradaResponse(
                        "INC-1", "CON-011", "VIA-2026-00045"), false));

        String cuerpo = """
                { "viajeId": "VIA-2026-00045", "tipo": "DOCUMENTARIA",
                  "descripcion": "Retencion SUTRAN por guia incompleta.", "atribuible": true }
                """;

        mockMvc.perform(post("/internal/v1/conductores/CON-011/incidencias")
                        .header("Idempotency-Key", "VIA-2026-00045:CON-011:incidencia")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidenciaId").value("INC-1"))
                .andExpect(jsonPath("$.viajeId").value("VIA-2026-00045"));
    }

    /**
     * {@code atribuible} es {@code Boolean} y no {@code boolean} justamente para esto: con el
     * primitivo, omitirlo lo dejaria en {@code false} y la incidencia constaria como no atribuible
     * sin que nadie lo haya dicho.
     */
    @Test
    @DisplayName("contrato 6: omitir atribuible es 400, no un false silencioso")
    void atribuibleOmitido() throws Exception {
        String cuerpo = """
                { "viajeId": "VIA-1", "tipo": "DOCUMENTARIA", "descripcion": "Sin el campo." }
                """;

        mockMvc.perform(post("/internal/v1/conductores/CON-011/incidencias")
                        .header("Idempotency-Key", "k-2")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.atribuible").exists());
    }
}
