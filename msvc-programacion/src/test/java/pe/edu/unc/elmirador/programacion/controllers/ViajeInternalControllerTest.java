package pe.edu.unc.elmirador.programacion.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.unc.elmirador.programacion.dto.internal.response.HojaDeRutaContratoResponse;
import pe.edu.unc.elmirador.programacion.dto.internal.response.ParadaContratoResponse;
import pe.edu.unc.elmirador.programacion.dto.internal.response.UbicacionContratoResponse;
import pe.edu.unc.elmirador.programacion.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.programacion.exceptions.TransicionDeViajeInvalidaException;
import pe.edu.unc.elmirador.programacion.services.ViajeInternalService;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ViajeInternalController.class)
class ViajeInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ViajeInternalService servicio;

    @Test
    void obtenerHojaDeRuta_Devuelve200ConEstructuraExacta() throws Exception {
        ParadaContratoResponse p1 = new ParadaContratoResponse(
                1, "CARGA", "ORD-100",
                new UbicacionContratoResponse("Jr. Ayacucho 450", "Cajamarca", "Almacen 2", "+51 976 000 111"),
                null
        );

        HojaDeRutaContratoResponse response = new HojaDeRutaContratoResponse(
                "V-1234", "PROGRAMADO", "U-99", List.of("C-88", "C-89"),
                "Coordinar con almacen del cliente antes de las 07:00.", List.of(p1)
        );

        when(servicio.obtenerHojaDeRutaEjecutable("V-1234")).thenReturn(response);

        mockMvc.perform(get("/internal/v1/viajes/V-1234/hoja-de-ruta"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.viajeId").value("V-1234"))
                .andExpect(jsonPath("$.estado").value("PROGRAMADO"))
                .andExpect(jsonPath("$.unidadId").value("U-99"))
                .andExpect(jsonPath("$.conductorIds[0]").value("C-88"))
                .andExpect(jsonPath("$.conductorIds[1]").value("C-89"))
                .andExpect(jsonPath("$.paradas[0].secuencia").value(1))
                .andExpect(jsonPath("$.paradas[0].tipo").value("CARGA"))
                .andExpect(jsonPath("$.paradas[0].ordenDeServicioId").value("ORD-100"))
                .andExpect(jsonPath("$.paradas[0].ubicacion.direccion").value("Jr. Ayacucho 450"))
                .andExpect(jsonPath("$.paradas[0].ubicacion.distrito").value("Cajamarca"))
                .andExpect(jsonPath("$.paradas[0].ubicacion.referencia").value("Almacen 2"))
                .andExpect(jsonPath("$.paradas[0].ubicacion.contacto").value("+51 976 000 111"))
                .andExpect(jsonPath("$.observaciones").value("Coordinar con almacen del cliente antes de las 07:00."));
    }

    @Test
    void obtenerHojaDeRuta_Devuelve404SiNoExiste() throws Exception {
        when(servicio.obtenerHojaDeRutaEjecutable("V-999"))
                .thenThrow(new RecursoNoEncontradoException("viaje", "V-999"));

        mockMvc.perform(get("/internal/v1/viajes/V-999/hoja-de-ruta"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/recurso-no-encontrado"));
    }

    @Test
    void obtenerHojaDeRuta_Devuelve409SiEstaPlanificado() throws Exception {
        when(servicio.obtenerHojaDeRutaEjecutable("V-001"))
                .thenThrow(new TransicionDeViajeInvalidaException("El viaje V-001 esta en PLANIFICADO"));

        mockMvc.perform(get("/internal/v1/viajes/V-001/hoja-de-ruta"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/transicion-invalida"))
                .andExpect(jsonPath("$.detail").value("El viaje V-001 esta en PLANIFICADO"));
    }
}
