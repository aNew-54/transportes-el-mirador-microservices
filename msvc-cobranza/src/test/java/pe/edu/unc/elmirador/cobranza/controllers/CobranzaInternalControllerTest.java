package pe.edu.unc.elmirador.cobranza.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.edu.unc.elmirador.cobranza.dto.internal.request.CondicionDePagoRequest;
import pe.edu.unc.elmirador.cobranza.dto.internal.request.CrearCuentaPorCobrarRequest;
import pe.edu.unc.elmirador.cobranza.dto.internal.request.DetraccionRequest;
import pe.edu.unc.elmirador.cobranza.dto.internal.request.ImporteRequest;
import pe.edu.unc.elmirador.cobranza.dto.internal.response.CuentaPorCobrarCreadaResponse;
import pe.edu.unc.elmirador.cobranza.dto.internal.response.EstadoCrediticioResponse;
import pe.edu.unc.elmirador.cobranza.dto.response.ResultadoIdempotente;
import pe.edu.unc.elmirador.cobranza.exceptions.ImportesInconsistentesException;
import pe.edu.unc.elmirador.cobranza.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.cobranza.services.CobranzaInternalService;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(CobranzaInternalController.class)
class CobranzaInternalControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockitoBean
    private CobranzaInternalService servicio;

    @Test
    void estadoCrediticio_Retorna200() throws Exception {
        EstadoCrediticioResponse response = new EstadoCrediticioResponse(
                "CLI-0007",
                "SUSPENDIDO",
                LocalDate.parse("2026-08-28"),
                43,
                2,
                List.of(
                        new ImporteRequest("5420.30", "PEN"),
                        new ImporteRequest("800.00", "USD")
                )
        );

        when(servicio.estadoCrediticio("CLI-0007")).thenReturn(response);

        mvc.perform(get("/internal/v1/clientes/CLI-0007/estado-crediticio")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.clienteId").value("CLI-0007"))
                .andExpect(jsonPath("$.situacion").value("SUSPENDIDO"))
                .andExpect(jsonPath("$.fechaDeCambio").value("2026-08-28"))
                .andExpect(jsonPath("$.diasDeAtrasoMaximo").value(43))
                .andExpect(jsonPath("$.cuentasVencidas").value(2))
                .andExpect(jsonPath("$.deudaPorMoneda[0].monto").value("5420.30"))
                .andExpect(jsonPath("$.deudaPorMoneda[0].moneda").value("PEN"))
                .andExpect(jsonPath("$.deudaPorMoneda[1].monto").value("800.00"))
                .andExpect(jsonPath("$.deudaPorMoneda[1].moneda").value("USD"));
    }
    
    @Test
    void estadoCrediticio_NoExiste_404() throws Exception {
        when(servicio.estadoCrediticio("CLI-0007")).thenThrow(new RecursoNoEncontradoException("cliente", "CLI-0007"));

        mvc.perform(get("/internal/v1/clientes/CLI-0007/estado-crediticio"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void crearCuenta_Retorna201() throws Exception {
        CrearCuentaPorCobrarRequest peticion = new CrearCuentaPorCobrarRequest(
                "FAC-2026-000310",
                "F001-00000310",
                "CLI-0007",
                new ImporteRequest("1821.60", "PEN"),
                new DetraccionRequest(BigDecimal.valueOf(4), "72.86", "PEN", "00-123-456789"),
                new ImporteRequest("1748.74", "PEN"),
                OffsetDateTime.parse("2026-09-10T16:30:00-05:00"),
                OffsetDateTime.parse("2026-10-10T23:59:59-05:00"),
                new CondicionDePagoRequest("CREDITO", 30)
        );

        ResultadoIdempotente<CuentaPorCobrarCreadaResponse> resultado = new ResultadoIdempotente<>(
                new CuentaPorCobrarCreadaResponse("FAC-2026-000310", "cuenta-123"), false);

        when(servicio.crearCuentaPorCobrar(eq("FAC-2026-000310"), any())).thenReturn(resultado);

        mvc.perform(post("/internal/v1/cuentas-por-cobrar")
                .header("Idempotency-Key", "FAC-2026-000310")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(peticion)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.facturaId").value("FAC-2026-000310"))
                .andExpect(jsonPath("$.cuentaId").value("cuenta-123"));
    }
    
    @Test
    void crearCuenta_MismaClave_Retorna200() throws Exception {
        CrearCuentaPorCobrarRequest peticion = new CrearCuentaPorCobrarRequest(
                "FAC-2026-000310",
                "F001-00000310",
                "CLI-0007",
                new ImporteRequest("1821.60", "PEN"),
                new DetraccionRequest(BigDecimal.valueOf(4), "72.86", "PEN", "00-123-456789"),
                new ImporteRequest("1748.74", "PEN"),
                OffsetDateTime.parse("2026-09-10T16:30:00-05:00"),
                OffsetDateTime.parse("2026-10-10T23:59:59-05:00"),
                new CondicionDePagoRequest("CREDITO", 30)
        );

        ResultadoIdempotente<CuentaPorCobrarCreadaResponse> resultado = new ResultadoIdempotente<>(
                new CuentaPorCobrarCreadaResponse("FAC-2026-000310", "cuenta-123"), true);

        when(servicio.crearCuentaPorCobrar(eq("FAC-2026-000310"), any())).thenReturn(resultado);

        mvc.perform(post("/internal/v1/cuentas-por-cobrar")
                .header("Idempotency-Key", "FAC-2026-000310")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(peticion)))
                .andExpect(status().isOk());
    }

    @Test
    void crearCuenta_SinIdempotencyKey_400() throws Exception {
        CrearCuentaPorCobrarRequest peticion = new CrearCuentaPorCobrarRequest(
                "FAC-2026-000310",
                "F001-00000310",
                "CLI-0007",
                new ImporteRequest("1821.60", "PEN"),
                new DetraccionRequest(BigDecimal.valueOf(4), "72.86", "PEN", "00-123-456789"),
                new ImporteRequest("1748.74", "PEN"),
                OffsetDateTime.parse("2026-09-10T16:30:00-05:00"),
                OffsetDateTime.parse("2026-10-10T23:59:59-05:00"),
                new CondicionDePagoRequest("CREDITO", 30)
        );

        mvc.perform(post("/internal/v1/cuentas-por-cobrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(peticion)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crearCuenta_FallaFAC04_422() throws Exception {
        CrearCuentaPorCobrarRequest peticion = new CrearCuentaPorCobrarRequest(
                "FAC-2026-000310",
                "F001-00000310",
                "CLI-0007",
                new ImporteRequest("1821.60", "PEN"),
                new DetraccionRequest(BigDecimal.valueOf(4), "72.86", "PEN", "00-123-456789"),
                new ImporteRequest("1700.00", "PEN"),
                OffsetDateTime.parse("2026-09-10T16:30:00-05:00"),
                OffsetDateTime.parse("2026-10-10T23:59:59-05:00"),
                new CondicionDePagoRequest("CREDITO", 30)
        );

        when(servicio.crearCuentaPorCobrar(eq("FAC-2026-000310"), any()))
                .thenThrow(new ImportesInconsistentesException("Violacion FAC-04"));

        mvc.perform(post("/internal/v1/cuentas-por-cobrar")
                .header("Idempotency-Key", "FAC-2026-000310")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(peticion)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Unprocessable Entity"));
    }
}
