package pe.edu.unc.elmirador.programacion.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.unc.elmirador.programacion.dto.response.CargaConsolidadaResponse;
import pe.edu.unc.elmirador.programacion.dto.response.CargaResponse;
import pe.edu.unc.elmirador.programacion.dto.response.HojaDeRutaResponse;
import pe.edu.unc.elmirador.programacion.dto.response.RutaResponse;
import pe.edu.unc.elmirador.programacion.dto.response.VentanaDeTiempoResponse;
import pe.edu.unc.elmirador.programacion.dto.response.ViajeResponse;
import pe.edu.unc.elmirador.programacion.models.vo.EstadoDeViaje;
import pe.edu.unc.elmirador.programacion.models.vo.TipoDeCarga;
import pe.edu.unc.elmirador.programacion.services.ViajeService;

@WebMvcTest(ViajeInternalController.class)
class ViajeInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ViajeService servicio;

    @Test
    @DisplayName("GET /internal/v1/viajes/{id}/hoja-de-ruta 409 para un viaje en PLANIFICADO")
    void hojaDeRuta409ParaPlanificado() throws Exception {
        ViajeResponse viaje = new ViajeResponse(
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

        when(servicio.consultar("v-1")).thenReturn(viaje);

        mockMvc.perform(get("/internal/v1/viajes/v-1/hoja-de-ruta"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/transicion-invalida"));
    }
    
    @Test
    @DisplayName("GET /internal/v1/viajes/{id}/hoja-de-ruta 200")
    void hojaDeRuta200() throws Exception {
        ViajeResponse viaje = new ViajeResponse(
                "v-1",
                new RutaResponse("Lima", "Arequipa", "Sur"),
                new VentanaDeTiempoResponse(OffsetDateTime.parse("2026-03-10T10:00:00Z"), OffsetDateTime.parse("2026-03-11T10:00:00Z")),
                new CargaConsolidadaResponse(List.of(
                        new CargaResponse("ord-1", 1000, new BigDecimal("2.5"), TipoDeCarga.PALETIZADA, 1)
                ), 1000, new BigDecimal("2.5")),
                null,
                EstadoDeViaje.PROGRAMADO,
                new HojaDeRutaResponse(List.of()),
                List.of("ord-1")
        );

        when(servicio.consultar("v-1")).thenReturn(viaje);

        mockMvc.perform(get("/internal/v1/viajes/v-1/hoja-de-ruta"))
                .andExpect(status().isOk());
    }
}
