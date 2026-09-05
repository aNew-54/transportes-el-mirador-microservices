package pe.edu.unc.elmirador.comercial.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.edu.unc.elmirador.comercial.dto.internal.response.DiferenciaRegistradaResponse;
import pe.edu.unc.elmirador.comercial.dto.internal.response.EsperaRegistradaResponse;
import pe.edu.unc.elmirador.comercial.dto.internal.response.OrdenConfirmadaResponse;
import pe.edu.unc.elmirador.comercial.dto.internal.response.SnapshotFacturableResponse;
import pe.edu.unc.elmirador.comercial.dto.response.ResultadoIdempotente;
import pe.edu.unc.elmirador.comercial.exceptions.TransicionDeOrdenInvalidaException;
import pe.edu.unc.elmirador.comercial.services.ComercialInternalService;

@WebMvcTest(ComercialInternalController.class)
class ComercialInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComercialInternalService servicio;

    @Test
    @DisplayName("contrato 1: JSON de orden confirmada")
    void formaContrato1() throws Exception {
        when(servicio.consultarOrdenConfirmada("ORD-2026-000123")).thenReturn(
                new OrdenConfirmadaResponse(
                        "ORD-2026-000123", "CLI-0007", "CONFIRMADA",
                        new OrdenConfirmadaResponse.CargaResponse(8500, new BigDecimal("24.5"), "PALLETS", "ALIMENTARIA"),
                        new OrdenConfirmadaResponse.RutaResponse("Cajamarca", "Trujillo", "COSTA_NORTE", 296),
                        new OrdenConfirmadaResponse.VentanaResponse("2026-09-10T06:00:00-05:00", "2026-09-10T18:00:00-05:00"),
                        true, List.of("SOLO_CARGA_ALIMENTARIA"), "FURGON"
                )
        );

        mockMvc.perform(get("/internal/v1/ordenes/ORD-2026-000123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordenId").value("ORD-2026-000123"))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.carga.pesoKg").value(8500))
                .andExpect(jsonPath("$.carga.embalaje").value("PALLETS"))
                .andExpect(jsonPath("$.ruta.origen").value("Cajamarca"))
                .andExpect(jsonPath("$.ventana.inicio").value("2026-09-10T06:00:00-05:00"))
                .andExpect(jsonPath("$.permiteConsolidacion").value(true))
                .andExpect(jsonPath("$.restriccionesConsolidacion[0]").value("SOLO_CARGA_ALIMENTARIA"))
                .andExpect(jsonPath("$.tipoUnidadRequerido").value("FURGON"));
    }

    @Test
    @DisplayName("contrato 1: orden que existe y no esta confirmada devuelve 409")
    void ordenNoConfirmada409() throws Exception {
        when(servicio.consultarOrdenConfirmada("ORD-2026-000123"))
                .thenThrow(new TransicionDeOrdenInvalidaException("La orden no esta confirmada"));

        mockMvc.perform(get("/internal/v1/ordenes/ORD-2026-000123"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/transicion-invalida"));
    }

    @Test
    @DisplayName("contrato 9: JSON de snapshot facturable")
    void formaContrato9() throws Exception {
        when(servicio.consultarSnapshotFacturable("ORD-2026-000123")).thenReturn(
                new SnapshotFacturableResponse(
                        "ORD-2026-000123", "CLI-0007", "20481234567", "Distribuidora Norte S.A.C.",
                        new SnapshotFacturableResponse.TarifaResponse(
                                new SnapshotFacturableResponse.DineroResponse("1800.00", "PEN"),
                                List.of(new SnapshotFacturableResponse.RecargoResponse("SOBRECAPACIDAD", new BigDecimal("10"))),
                                new SnapshotFacturableResponse.DescuentoResponse(new BigDecimal("8"), "CONSOLIDACION"),
                                new SnapshotFacturableResponse.DineroResponse("1821.60", "PEN")
                        ),
                        new SnapshotFacturableResponse.CondicionDePagoResponse("CREDITO", 30),
                        OffsetDateTime.parse("2026-09-10T16:00:00-05:00")
                )
        );

        mockMvc.perform(get("/internal/v1/ordenes/ORD-2026-000123/snapshot-facturable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordenId").value("ORD-2026-000123"))
                .andExpect(jsonPath("$.ruc").value("20481234567"))
                .andExpect(jsonPath("$.tarifa.fleteBase.monto").value("1800.00"))
                .andExpect(jsonPath("$.tarifa.total.monto").value("1821.60"));
    }

    @Test
    @DisplayName("contrato 7: sin cabecera Idempotency-Key devuelve 400")
    void sinCabeceraIdempotencia400() throws Exception {
        String req = "{ \"viajeId\": \"VIA-1\", \"declarado\": { \"pesoKg\": 10, \"volumenM3\": 1, \"embalaje\": \"S\" }, \"real\": { \"pesoKg\": 10, \"volumenM3\": 1, \"embalaje\": \"S\" }, \"decision\": \"ACEPTADA\", \"momento\": \"2026-09-10T06:55:00-05:00\" }";
        mockMvc.perform(post("/internal/v1/ordenes/ORD-1/diferencias-de-carga")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(req))
                .andExpect(status().isBadRequest());
    }
}
