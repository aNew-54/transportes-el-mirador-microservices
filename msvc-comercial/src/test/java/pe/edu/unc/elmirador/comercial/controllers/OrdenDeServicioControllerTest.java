package pe.edu.unc.elmirador.comercial.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import pe.edu.unc.elmirador.comercial.dto.request.CancelarOrdenRequest;
import pe.edu.unc.elmirador.comercial.dto.request.CrearOrdenRequest;
import pe.edu.unc.elmirador.comercial.dto.response.CondicionDePagoResponse;
import pe.edu.unc.elmirador.comercial.dto.response.OrdenDeServicioResponse;
import pe.edu.unc.elmirador.comercial.dto.response.TarifaResponse;
import pe.edu.unc.elmirador.comercial.exceptions.CobranzaIntegrationException;
import pe.edu.unc.elmirador.comercial.exceptions.CondicionDePagoInconsistenteException;
import pe.edu.unc.elmirador.comercial.exceptions.TransicionDeOrdenInvalidaException;
import pe.edu.unc.elmirador.comercial.models.vo.ModalidadDePago;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeCarga;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeUnidad;
import pe.edu.unc.elmirador.comercial.services.OrdenDeServicioService;

@WebMvcTest(OrdenDeServicioController.class)
class OrdenDeServicioControllerTest {

    private static final OffsetDateTime VENTANA_INICIO =
            OffsetDateTime.parse("2026-09-10T06:00:00-05:00");
    private static final OffsetDateTime VENTANA_FIN =
            OffsetDateTime.parse("2026-09-10T18:00:00-05:00");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrdenDeServicioService servicio;

    @Test
    void crear_ordenValida_devuelve201() throws Exception {
        CrearOrdenRequest request = new CrearOrdenRequest(
                "cli-1", "ctm-1", TipoDeUnidad.FURGON, 1000, new BigDecimal("10.00"), TipoDeCarga.GENERAL,
                "LIMA", "PIURA", "NORTE",
                "PALLETS", "ALIMENTARIA", 296,
                VENTANA_INICIO, VENTANA_FIN,
                ModalidadDePago.CONTADO, 0);

        OrdenDeServicioResponse response = new OrdenDeServicioResponse(
                "ord-1", "cli-1", null, 1000, new BigDecimal("10.00"), "GENERAL",
                "LIMA", "PIURA", "NORTE",
                new TarifaResponse(new BigDecimal("100.00"), "PEN", java.util.List.of(), null),
                new CondicionDePagoResponse("CONTADO", 0),
                "BORRADOR", null, null);

        when(servicio.crear(any())).thenReturn(response);

        mvc.perform(post("/api/v1/ordenes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/ordenes/ord-1"))
                .andExpect(jsonPath("$.id").value("ord-1"));
    }

    @Test
    void crear_condicionDePagoInconsistente_devuelve422() throws Exception {
        CrearOrdenRequest request = new CrearOrdenRequest(
                "cli-1", "ctm-1", TipoDeUnidad.FURGON, 1000, new BigDecimal("10.00"), TipoDeCarga.GENERAL,
                "LIMA", "PIURA", "NORTE",
                "PALLETS", "ALIMENTARIA", 296,
                VENTANA_INICIO, VENTANA_FIN,
                ModalidadDePago.CREDITO, 30);

        when(servicio.crear(any())).thenThrow(new CondicionDePagoInconsistenteException("Cliente suspendido"));

        mvc.perform(post("/api/v1/ordenes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/invariante-violada"))
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void confirmar_ordenValida_devuelve200() throws Exception {
        OrdenDeServicioResponse response = new OrdenDeServicioResponse(
                "ord-1", "cli-1", null, 1000, new BigDecimal("10.00"), "GENERAL",
                "LIMA", "PIURA", "NORTE",
                new TarifaResponse(new BigDecimal("100.00"), "PEN", java.util.List.of(), null),
                new CondicionDePagoResponse("CONTADO", 0),
                "CONFIRMADA", null, null);

        when(servicio.confirmar(eq("ord-1"))).thenReturn(response);

        mvc.perform(post("/api/v1/ordenes/ord-1/confirmar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
    }

    @Test
    void confirmar_transicionInvalida_devuelve409() throws Exception {
        when(servicio.confirmar(eq("ord-1"))).thenThrow(new TransicionDeOrdenInvalidaException("Estado invalido"));

        mvc.perform(post("/api/v1/ordenes/ord-1/confirmar"))
                .andExpect(status().isConflict())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/transicion-orden-invalida"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void cancelar_ordenValida_devuelve200() throws Exception {
        CancelarOrdenRequest request = new CancelarOrdenRequest("gerente");

        OrdenDeServicioResponse response = new OrdenDeServicioResponse(
                "ord-1", "cli-1", null, 1000, new BigDecimal("10.00"), "GENERAL",
                "LIMA", "PIURA", "NORTE",
                new TarifaResponse(new BigDecimal("100.00"), "PEN", java.util.List.of(), null),
                new CondicionDePagoResponse("CONTADO", 0),
                "CANCELADA", null, "gerente");

        when(servicio.cancelar(eq("ord-1"), any())).thenReturn(response);

        mvc.perform(post("/api/v1/ordenes/ord-1/cancelar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
    }

    /**
     * Contrato 11, comportamiento ante indisponibilidad. Es 503 y no 500: el defecto no esta en esta
     * peticion ni en este modulo. Y el detalle dice que no se pudo verificar, no «error interno»:
     * quien llama tiene que poder distinguir «no puedo comprobarlo» de «te lo deniego».
     */
    @Test
    void crear_conCobranzaCaida_devuelve503() throws Exception {
        CrearOrdenRequest request = new CrearOrdenRequest(
                "cli-1", "ctm-1", TipoDeUnidad.FURGON, 1000, new BigDecimal("10.00"), TipoDeCarga.GENERAL,
                "LIMA", "PIURA", "NORTE",
                "PALLETS", "ALIMENTARIA", 296,
                VENTANA_INICIO, VENTANA_FIN,
                ModalidadDePago.CREDITO, 30);

        when(servicio.crear(any())).thenThrow(new CobranzaIntegrationException(
                "Cobranza no respondio al consultar el estado crediticio del cliente cli-1"));

        mvc.perform(post("/api/v1/ordenes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.type")
                        .value("https://elmirador.unc.edu.pe/problems/estado-crediticio-no-verificable"))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.detail").value(
                        "Cobranza no respondio al consultar el estado crediticio del cliente cli-1"));
    }

    @Test
    void cancelar_transicionInvalida_devuelve409() throws Exception {
        CancelarOrdenRequest request = new CancelarOrdenRequest("gerente");

        when(servicio.cancelar(eq("ord-1"), any())).thenThrow(new TransicionDeOrdenInvalidaException("Ya cancelada"));

        mvc.perform(post("/api/v1/ordenes/ord-1/cancelar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/transicion-orden-invalida"))
                .andExpect(jsonPath("$.status").value(409));
    }
}
