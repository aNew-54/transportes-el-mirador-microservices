package pe.edu.unc.elmirador.facturacion.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.edu.unc.elmirador.facturacion.dto.response.FacturaResponse;
import pe.edu.unc.elmirador.facturacion.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.facturacion.exceptions.EmisionSinConformidadException;
import pe.edu.unc.elmirador.facturacion.exceptions.ImportesInconsistentesException;
import pe.edu.unc.elmirador.facturacion.exceptions.DominioFacturacionException;
import pe.edu.unc.elmirador.facturacion.exceptions.FacturaInmutableException;
import pe.edu.unc.elmirador.facturacion.exceptions.IncidenciaSinResolverException;
import pe.edu.unc.elmirador.facturacion.exceptions.MonedaIncompatibleException;
import pe.edu.unc.elmirador.facturacion.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.facturacion.services.FacturaService;
import java.util.List;

@WebMvcTest(FacturaController.class)
class FacturaControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private FacturaService servicio;

    @Test
    @DisplayName("POST /facturas devuelve 201")
    void abrir201() throws Exception {
        FacturaResponse resp = new FacturaResponse("f-1", "ord-1", "cli-1", null, null, null, null, null, null, false, null, null, null, null, null, null, List.of(), List.of());
        when(servicio.abrir(any())).thenReturn(resp);
        String json = """
            {
              "ordenDeServicioId": "ord-1",
              "clienteId": "cli-1",
              "snapshot": { "tarifaMonto": 100, "codigoMoneda": "PEN", "obtenidoEn": "2026-03-10T10:00:00Z" },
              "detraccion": { "porcentaje": 0, "monto": 0 }
            }
            """;
        mockMvc.perform(post("/api/v1/facturas").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/facturas/f-1"));
    }

    @Test
    @DisplayName("POST /facturas con orden repetida devuelve 409")
    void abrir409() throws Exception {
        when(servicio.abrir(any())).thenThrow(new ConflictoDeRecursoException("Ya existe"));
        String json = """
            {
              "ordenDeServicioId": "ord-1",
              "clienteId": "cli-1",
              "snapshot": { "tarifaMonto": 100, "codigoMoneda": "PEN", "obtenidoEn": "2026-03-10T10:00:00Z" },
              "detraccion": { "porcentaje": 0, "monto": 0 }
            }
            """;
        mockMvc.perform(post("/api/v1/facturas").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/conflicto-de-recurso"));
    }

    @Test
    @DisplayName("POST /facturas/{id}/emitir devuelve 200")
    void emitir200() throws Exception {
        FacturaResponse resp = new FacturaResponse("f-1", "ord-1", "cli-1", null, null, null, null, null, null, false, null, null, null, null, null, null, List.of(), List.of());
        when(servicio.emitir(eq("f-1"), any())).thenReturn(resp);
        String json = """
            { "serie": "F001", "correlativo": 1 }
            """;
        mockMvc.perform(post("/api/v1/facturas/f-1/emitir").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /facturas/{id}/emitir sin conformidad devuelve 409")
    void emitir409() throws Exception {
        when(servicio.emitir(eq("f-1"), any())).thenThrow(new EmisionSinConformidadException("Sin conformidad"));
        String json = """
            { "serie": "F001", "correlativo": 1 }
            """;
        mockMvc.perform(post("/api/v1/facturas/f-1/emitir").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/emision-sin-conformidad"));
    }

    @Test
    @DisplayName("POST /facturas/{id}/emitir con importes inconsistentes devuelve 422")
    void emitir422() throws Exception {
        when(servicio.emitir(eq("f-1"), any())).thenThrow(new ImportesInconsistentesException("Inconsistente"));
        String json = """
            { "serie": "F001", "correlativo": 1 }
            """;
        mockMvc.perform(post("/api/v1/facturas/f-1/emitir").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/importes-inconsistentes"));
    }

    @Test
    @DisplayName("GET /facturas/{id} devuelve 200")
    void get200() throws Exception {
        FacturaResponse resp = new FacturaResponse("f-1", "ord-1", "cli-1", null, null, null, null, null, null, false, null, null, null, null, null, null, List.of(), List.of());
        when(servicio.porId("f-1")).thenReturn(resp);
        mockMvc.perform(get("/api/v1/facturas/f-1"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /facturas/{id} devuelve 404")
    void get404() throws Exception {
        when(servicio.porId("f-2")).thenThrow(new RecursoNoEncontradoException("factura", "f-2"));
        mockMvc.perform(get("/api/v1/facturas/f-2"))
            .andExpect(status().isNotFound());
    }

    /**
     * FAC-05: una incidencia sin resolver bloquea la emision. Es «ahora no» —resuelta la incidencia,
     * la misma peticion vale—, asi que 409 y no 422.
     */
    @Test
    @DisplayName("POST /facturas/{id}/emitir con una incidencia sin resolver devuelve 409")
    void emitirConIncidencia409() throws Exception {
        when(servicio.emitir(eq("f-1"), any()))
            .thenThrow(new IncidenciaSinResolverException("Queda una incidencia de faltante sin resolver"));

        mockMvc.perform(post("/api/v1/facturas/f-1/emitir")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"serie\":\"F001\",\"correlativo\":123}"))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/incidencia-sin-resolver"));
    }

    /** FAC-03: una factura emitida es inmutable; anularla dos veces es «ahora no». */
    @Test
    @DisplayName("POST /facturas/{id}/anular devuelve 200")
    void anular200() throws Exception {
        when(servicio.anular("f-1")).thenReturn(facturaDeEjemplo());

        mockMvc.perform(post("/api/v1/facturas/f-1/anular"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("f-1"));
    }

    @Test
    @DisplayName("POST /facturas/{id}/anular sobre una factura inmutable devuelve 409")
    void anular409() throws Exception {
        when(servicio.anular("f-1"))
            .thenThrow(new FacturaInmutableException("La factura ya esta anulada"));

        mockMvc.perform(post("/api/v1/facturas/f-1/anular"))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/factura-inmutable"))
            .andExpect(jsonPath("$.title").value("Conflict"))
            .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("GET /facturas pasa los tres filtros al servicio")
    void listar200() throws Exception {
        when(servicio.listar(any(), eq("cli-1"), any())).thenReturn(List.of(facturaDeEjemplo()));

        mockMvc.perform(get("/api/v1/facturas").param("clienteId", "cli-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("POST /facturas/falso-flete devuelve 201 con Location")
    void falsoFlete201() throws Exception {
        when(servicio.emitirFalsoFlete(any())).thenReturn(facturaDeEjemplo());

        String json = """
            {
              "ordenDeServicioId": "ord-9",
              "clienteId": "cli-1",
              "snapshot": { "tarifaMonto": 350.00, "codigoMoneda": "PEN", "obtenidoEn": "2026-03-10T10:00:00Z" },
              "detraccion": { "porcentaje": 0, "monto": 0 },
              "serie": "F001",
              "correlativo": 900,
              "descripcionLinea": "Falso flete por cancelacion posterior al despacho",
              "importeMonto": 350.00
            }
            """;

        mockMvc.perform(post("/api/v1/facturas/falso-flete")
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/facturas/f-1"));
    }

    @Test
    @DisplayName("POST /facturas/falso-flete sobre una orden no despachada devuelve 409")
    void falsoFlete409() throws Exception {
        when(servicio.emitirFalsoFlete(any()))
            .thenThrow(new ConflictoDeRecursoException("La orden no llego a despacharse"));

        String json = """
            {
              "ordenDeServicioId": "ord-9",
              "clienteId": "cli-1",
              "snapshot": { "tarifaMonto": 350.00, "codigoMoneda": "PEN", "obtenidoEn": "2026-03-10T10:00:00Z" },
              "detraccion": { "porcentaje": 0, "monto": 0 },
              "serie": "F001",
              "correlativo": 900,
              "descripcionLinea": "Falso flete por cancelacion posterior al despacho",
              "importeMonto": 350.00
            }
            """;

        mockMvc.perform(post("/api/v1/facturas/falso-flete")
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("un cuerpo que no supera la validacion de forma devuelve 400 con el detalle por campo")
    void abrir400() throws Exception {
        mockMvc.perform(post("/api/v1/facturas")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/validacion"))
            .andExpect(jsonPath("$.errores").exists());
    }

    /**
     * El comodin del manejador. No se alcanza por HTTP porque las siete excepciones de este contexto
     * estan declaradas una a una; se prueba contra el manejador directamente para que una excepcion
     * de dominio nueva que nadie recuerde declarar siga siendo 422 y nunca un 500.
     */
    @Test
    @DisplayName("una invariante rota que nadie declaro sigue siendo 422 y no 500")
    void comodinEs422() {
        ProblemDetail problema = new ManejadorDeErrores()
            .invariante(new DominioFacturacionException("Una regla nueva que nadie declaro"));

        assertThat(problema.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(problema.getType()).hasToString("https://elmirador.unc.edu.pe/problems/invariante-violada");
    }

    private FacturaResponse facturaDeEjemplo() {
        return new FacturaResponse("f-1", "ord-1", "cli-1", null, null, null, null, null, null,
                false, null, null, null, null, null, null, List.of(), List.of());
    }
}
