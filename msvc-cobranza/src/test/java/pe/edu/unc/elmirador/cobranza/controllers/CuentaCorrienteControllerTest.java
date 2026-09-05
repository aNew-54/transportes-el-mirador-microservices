package pe.edu.unc.elmirador.cobranza.controllers;

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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import pe.edu.unc.elmirador.cobranza.dto.response.CuentaCorrienteResponse;
import pe.edu.unc.elmirador.cobranza.dto.response.CuentaPorCobrarResponse;
import pe.edu.unc.elmirador.cobranza.dto.response.ImporteResponse;
import pe.edu.unc.elmirador.cobranza.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.cobranza.exceptions.RehabilitacionInvalidaException;
import pe.edu.unc.elmirador.cobranza.models.vo.EstadoDeDocumento;
import pe.edu.unc.elmirador.cobranza.models.vo.SituacionCrediticia;
import pe.edu.unc.elmirador.cobranza.services.CuentaCorrienteService;

@WebMvcTest(CuentaCorrienteController.class)
class CuentaCorrienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @MockitoBean
    private CuentaCorrienteService servicio;

    @Test
    @DisplayName("GET /cuentas-corrientes/{clienteId} devuelve 200")
    void porId200() throws Exception {
        CuentaCorrienteResponse respuesta = new CuentaCorrienteResponse(
                "cli-1", SituacionCrediticia.VIGENTE, null, LocalDate.of(2026, 1, 1),
                List.of(new ImporteResponse(new BigDecimal("100.00"), "PEN")), 0, 0, List.of()
        );
        when(servicio.porClienteId("cli-1")).thenReturn(respuesta);

        mockMvc.perform(get("/api/v1/cuentas-corrientes/cli-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clienteId").value("cli-1"));
    }

    @Test
    @DisplayName("GET /cuentas-corrientes/{clienteId} inexistente devuelve 404")
    void porId404() throws Exception {
        when(servicio.porClienteId("no-existe"))
                .thenThrow(new RecursoNoEncontradoException("cliente", "no-existe"));

        mockMvc.perform(get("/api/v1/cuentas-corrientes/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/recurso-no-encontrado"));
    }

    @Test
    @DisplayName("GET /cuentas-por-cobrar devuelve 200")
    void listar200() throws Exception {
        when(servicio.listarCuentasPorCobrar(null, null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/cuentas-por-cobrar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("POST /cuentas-por-cobrar/{id}/detraccion devuelve 200")
    void registrarDetraccion200() throws Exception {
        CuentaPorCobrarResponse res = new CuentaPorCobrarResponse("cpc-1", "cli-1", "fac-1", "doc-1",
                new BigDecimal("100.00"), "PEN", new BigDecimal("10.00"), "PEN", new BigDecimal("0.00"), "PEN",
                new BigDecimal("90.00"), "PEN", new BigDecimal("90.00"), "PEN", LocalDate.of(2026, 1, 1),
                true, EstadoDeDocumento.VIGENTE, 0);
        when(servicio.registrarDetraccion("cpc-1")).thenReturn(res);

        mockMvc.perform(post("/api/v1/cuentas-por-cobrar/cpc-1/detraccion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detraccionDepositada").value(true));
    }

    @Test
    @DisplayName("POST /cuentas-corrientes/{clienteId}/rehabilitar con cuentas vencidas devuelve 409")
    void rehabilitar409() throws Exception {
        when(servicio.rehabilitar("cli-1"))
                .thenThrow(new RehabilitacionInvalidaException("Existen cuentas con mas de 30 dias de atraso"));

        mockMvc.perform(post("/api/v1/cuentas-corrientes/cli-1/rehabilitar"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/rehabilitacion-invalida"));
    }

}
