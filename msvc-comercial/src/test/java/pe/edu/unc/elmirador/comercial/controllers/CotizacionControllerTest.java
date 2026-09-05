package pe.edu.unc.elmirador.comercial.controllers;

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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import pe.edu.unc.elmirador.comercial.dto.request.AceptarCotizacionRequest;
import pe.edu.unc.elmirador.comercial.dto.request.EmitirCotizacionRequest;
import pe.edu.unc.elmirador.comercial.dto.request.RechazarCotizacionRequest;
import pe.edu.unc.elmirador.comercial.dto.response.CotizacionResponse;
import pe.edu.unc.elmirador.comercial.dto.response.TarifaResponse;
import pe.edu.unc.elmirador.comercial.exceptions.CotizacionVencidaException;
import pe.edu.unc.elmirador.comercial.exceptions.DescuentoNoAutorizadoException;
import pe.edu.unc.elmirador.comercial.models.vo.ModalidadDePago;
import pe.edu.unc.elmirador.comercial.models.vo.MotivoDeRechazo;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeCarga;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeUnidad;
import pe.edu.unc.elmirador.comercial.services.CotizacionService;

@WebMvcTest(CotizacionController.class)
class CotizacionControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CotizacionService servicio;

    @Test
    void emitir_valido_devuelve201() throws Exception {
        EmitirCotizacionRequest request = new EmitirCotizacionRequest(
                "cli-1", 1000, new BigDecimal("10.00"), TipoDeCarga.GENERAL,
                "LIMA", "PIURA", "NORTE", TipoDeUnidad.FURGON, null, null);

        CotizacionResponse response = new CotizacionResponse(
                "cot-1", "cli-1", "tar-1", 1000, new BigDecimal("10.00"), "GENERAL",
                "LIMA", "PIURA", "NORTE",
                new TarifaResponse(new BigDecimal("1000.00"), "PEN", List.of(), null),
                LocalDate.parse("2026-09-04"), LocalDate.parse("2026-09-10"),
                "EMITIDA", null);

        when(servicio.emitir(any())).thenReturn(response);

        mvc.perform(post("/api/v1/cotizaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/cotizaciones/cot-1"))
                .andExpect(jsonPath("$.id").value("cot-1"));
    }

    @Test
    void emitir_invalido_devuelve400() throws Exception {
        // Falta clienteId (NotBlank)
        EmitirCotizacionRequest request = new EmitirCotizacionRequest(
                null, 1000, new BigDecimal("10.00"), TipoDeCarga.GENERAL,
                "LIMA", "PIURA", "NORTE", TipoDeUnidad.FURGON, null, null);

        mvc.perform(post("/api/v1/cotizaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/validacion"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void emitir_descuentoInvalido_devuelve422() throws Exception {
        EmitirCotizacionRequest request = new EmitirCotizacionRequest(
                "cli-1", 1000, new BigDecimal("10.00"), TipoDeCarga.GENERAL,
                "LIMA", "PIURA", "NORTE", TipoDeUnidad.FURGON, new BigDecimal("20.00"), null);

        when(servicio.emitir(any())).thenThrow(new DescuentoNoAutorizadoException("Descuento invalido"));

        mvc.perform(post("/api/v1/cotizaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/invariante-violada"))
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void aceptar_valido_devuelve200() throws Exception {
        AceptarCotizacionRequest request = new AceptarCotizacionRequest(ModalidadDePago.CREDITO, 30);

        CotizacionResponse response = new CotizacionResponse(
                "cot-1", "cli-1", "tar-1", 1000, new BigDecimal("10.00"), "GENERAL",
                "LIMA", "PIURA", "NORTE",
                new TarifaResponse(new BigDecimal("1000.00"), "PEN", List.of(), null),
                LocalDate.parse("2026-09-04"), LocalDate.parse("2026-09-10"),
                "ACEPTADA", null);

        when(servicio.aceptar(eq("cot-1"), any())).thenReturn(response);

        mvc.perform(post("/api/v1/cotizaciones/cot-1/aceptar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACEPTADA"));
    }

    @Test
    void aceptar_vencida_devuelve409() throws Exception {
        AceptarCotizacionRequest request = new AceptarCotizacionRequest(ModalidadDePago.CREDITO, 30);

        when(servicio.aceptar(eq("cot-1"), any())).thenThrow(new CotizacionVencidaException("Vencida"));

        mvc.perform(post("/api/v1/cotizaciones/cot-1/aceptar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/cotizacion-vencida"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void rechazar_valido_devuelve200() throws Exception {
        RechazarCotizacionRequest request = new RechazarCotizacionRequest(MotivoDeRechazo.PRECIO);

        CotizacionResponse response = new CotizacionResponse(
                "cot-1", "cli-1", "tar-1", 1000, new BigDecimal("10.00"), "GENERAL",
                "LIMA", "PIURA", "NORTE",
                new TarifaResponse(new BigDecimal("1000.00"), "PEN", List.of(), null),
                LocalDate.parse("2026-09-04"), LocalDate.parse("2026-09-10"),
                "RECHAZADA", "PRECIO");

        when(servicio.rechazar(eq("cot-1"), any())).thenReturn(response);

        mvc.perform(post("/api/v1/cotizaciones/cot-1/rechazar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.motivoDeRechazo").value("PRECIO"));
    }
}
