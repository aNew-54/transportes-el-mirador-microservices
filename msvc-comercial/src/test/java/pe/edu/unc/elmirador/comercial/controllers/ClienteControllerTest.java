package pe.edu.unc.elmirador.comercial.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import pe.edu.unc.elmirador.comercial.dto.request.RegistrarClienteRequest;
import pe.edu.unc.elmirador.comercial.dto.response.ClienteResponse;
import pe.edu.unc.elmirador.comercial.dto.response.CondicionDePagoResponse;
import pe.edu.unc.elmirador.comercial.dto.response.EstadoCrediticioResponse;
import pe.edu.unc.elmirador.comercial.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.comercial.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.comercial.exceptions.RucInvalidoException;
import pe.edu.unc.elmirador.comercial.models.vo.ModalidadDePago;
import pe.edu.unc.elmirador.comercial.services.ClienteService;

@WebMvcTest(ClienteController.class)
class ClienteControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClienteService servicio;

    @Test
    void registrar_clienteValido_devuelve201() throws Exception {
        RegistrarClienteRequest request = new RegistrarClienteRequest(
                "20123456789", "Acme S.A.", ModalidadDePago.CREDITO, 30);
        
        ClienteResponse response = new ClienteResponse(
                "cli-123", "20123456789", "Acme S.A.",
                new CondicionDePagoResponse("CREDITO", 30),
                new EstadoCrediticioResponse("VIGENTE", LocalDate.parse("2026-09-04"))
        );

        when(servicio.registrar(any())).thenReturn(response);

        mvc.perform(post("/api/v1/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/clientes/cli-123"))
                .andExpect(jsonPath("$.id").value("cli-123"));
    }

    @Test
    void registrar_rucInvalido_devuelve400() throws Exception {
        RegistrarClienteRequest request = new RegistrarClienteRequest(
                "99123456789", "Acme S.A.", ModalidadDePago.CREDITO, 30);
        
        when(servicio.registrar(any())).thenThrow(new RucInvalidoException("RUC debe empezar con 10, 15, 17 o 20"));

        mvc.perform(post("/api/v1/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/ruc-invalido"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void registrar_rucDuplicado_devuelve409() throws Exception {
        RegistrarClienteRequest request = new RegistrarClienteRequest(
                "20123456789", "Acme S.A.", ModalidadDePago.CREDITO, 30);
        
        when(servicio.registrar(any())).thenThrow(new ConflictoDeRecursoException("Ya existe"));

        mvc.perform(post("/api/v1/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/conflicto-de-recurso"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void porId_clienteExiste_devuelve200() throws Exception {
        ClienteResponse response = new ClienteResponse(
                "cli-123", "20123456789", "Acme S.A.",
                new CondicionDePagoResponse("CREDITO", 30),
                new EstadoCrediticioResponse("VIGENTE", LocalDate.parse("2026-09-04"))
        );

        when(servicio.porId("cli-123")).thenReturn(response);

        mvc.perform(get("/api/v1/clientes/cli-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("cli-123"));
    }

    @Test
    void porId_noExiste_devuelve404() throws Exception {
        when(servicio.porId("cli-999")).thenThrow(new RecursoNoEncontradoException("Cliente", "cli-999"));

        mvc.perform(get("/api/v1/clientes/cli-999"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://elmirador.unc.edu.pe/problems/recurso-no-encontrado"))
                .andExpect(jsonPath("$.status").value(404));
    }
}
