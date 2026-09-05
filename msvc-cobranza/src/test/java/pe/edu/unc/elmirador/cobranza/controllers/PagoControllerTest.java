package pe.edu.unc.elmirador.cobranza.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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

import pe.edu.unc.elmirador.cobranza.dto.request.AplicacionRequest;
import pe.edu.unc.elmirador.cobranza.dto.request.AplicarPagoRequest;
import pe.edu.unc.elmirador.cobranza.dto.request.RegistrarPagoRequest;
import pe.edu.unc.elmirador.cobranza.dto.response.PagoResponse;
import pe.edu.unc.elmirador.cobranza.exceptions.AplicacionExcedeElPagoException;
import pe.edu.unc.elmirador.cobranza.exceptions.PagoDeOtroClienteException;
import pe.edu.unc.elmirador.cobranza.models.vo.ModalidadDePago;
import pe.edu.unc.elmirador.cobranza.services.PagoService;

@WebMvcTest(PagoController.class)
class PagoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    @MockitoBean
    private PagoService servicio;

    @Test
    @DisplayName("POST /pagos devuelve 201 con Location")
    void registrar201() throws Exception {
        RegistrarPagoRequest req = new RegistrarPagoRequest("cli-1", new BigDecimal("100.00"), "PEN", ModalidadDePago.EFECTIVO, null);
        PagoResponse res = new PagoResponse("p-1", "cli-1", new BigDecimal("100.00"), "PEN", ModalidadDePago.EFECTIVO, null, LocalDate.now(), new BigDecimal("0.00"), "PEN", new BigDecimal("100.00"), "PEN", List.of());
        
        when(servicio.registrar(any())).thenReturn(res);

        mockMvc.perform(post("/api/v1/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/pagos/p-1"))
                .andExpect(jsonPath("$.id").value("p-1"));
    }

    @Test
    @DisplayName("POST /pagos/{id}/aplicaciones con cuenta de otro cliente devuelve 422")
    void aplicar422() throws Exception {
        AplicarPagoRequest req = new AplicarPagoRequest(List.of(new AplicacionRequest("cpc-1", new BigDecimal("100.00"), "PEN")));

        when(servicio.aplicarPago(eq("p-1"), any()))
                .thenThrow(new PagoDeOtroClienteException("El pago pertenece al cliente cli-1 pero la cuenta pertenece al cliente cli-2"));

        mockMvc.perform(post("/api/v1/pagos/p-1/aplicaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/pago-de-otro-cliente"));
    }

    @Test
    @DisplayName("POST /pagos/{id}/aplicaciones excede el pago devuelve 422")
    void aplicarExcedeElPago422() throws Exception {
        AplicarPagoRequest req = new AplicarPagoRequest(List.of(new AplicacionRequest("cpc-1", new BigDecimal("100.00"), "PEN")));

        when(servicio.aplicarPago(eq("p-1"), any()))
                .thenThrow(new AplicacionExcedeElPagoException("Excede pago"));

        mockMvc.perform(post("/api/v1/pagos/p-1/aplicaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/aplicacion-excede-el-pago"));
    }

    @Test
    @DisplayName("POST /pagos/{id}/aplicaciones con saldo insuficiente devuelve 422")
    void aplicarSaldoInsuficiente422() throws Exception {
        AplicarPagoRequest req = new AplicarPagoRequest(List.of(new AplicacionRequest("cpc-1", new BigDecimal("100.00"), "PEN")));

        when(servicio.aplicarPago(eq("p-1"), any()))
                .thenThrow(new pe.edu.unc.elmirador.cobranza.exceptions.SaldoInsuficienteException("Excede saldo"));

        mockMvc.perform(post("/api/v1/pagos/p-1/aplicaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/saldo-insuficiente"));
    }

    @Test
    @DisplayName("POST /pagos/{id}/aplicaciones con moneda incompatible devuelve 422")
    void aplicarMonedaIncompatible422() throws Exception {
        AplicarPagoRequest req = new AplicarPagoRequest(List.of(new AplicacionRequest("cpc-1", new BigDecimal("100.00"), "PEN")));

        when(servicio.aplicarPago(eq("p-1"), any()))
                .thenThrow(new pe.edu.unc.elmirador.cobranza.exceptions.MonedaIncompatibleException("Moneda incompatible"));

        mockMvc.perform(post("/api/v1/pagos/p-1/aplicaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/moneda-incompatible"));
    }

    @Test
    @DisplayName("una excepcion comodin sigue siendo 422")
    void comodin422() {
        org.springframework.http.ProblemDetail p = new ManejadorDeErrores()
                .invarianteViolada(new pe.edu.unc.elmirador.cobranza.exceptions.DominioCobranzaException("comodin"));
        org.assertj.core.api.Assertions.assertThat(p.getStatus()).isEqualTo(422);
        org.assertj.core.api.Assertions.assertThat(p.getType()).hasToString("https://elmirador.unc.edu.pe/problems/invariante-violada");
    }

    @Test
    @DisplayName("IllegalArgumentException se traduce a 400")
    void ilegalArgument400() {
        org.springframework.http.ProblemDetail p = new ManejadorDeErrores()
                .argumentoInvalido(new IllegalArgumentException("invalido"));
        org.assertj.core.api.Assertions.assertThat(p.getStatus()).isEqualTo(400);
        org.assertj.core.api.Assertions.assertThat(p.getType()).hasToString("https://elmirador.unc.edu.pe/problems/argumento-invalido");
    }
}
