package pe.edu.unc.elmirador.comercial.controllers;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.unc.elmirador.comercial.dto.request.RegistrarClienteRequest;
import pe.edu.unc.elmirador.comercial.dto.response.ClienteResponse;
import pe.edu.unc.elmirador.comercial.services.ClienteService;

@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteController {

    private final ClienteService servicio;

    public ClienteController(ClienteService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<ClienteResponse> registrar(@Valid @RequestBody RegistrarClienteRequest peticion) {
        ClienteResponse creado = servicio.registrar(peticion);
        return ResponseEntity
                .created(URI.create("/api/v1/clientes/" + creado.id()))
                .body(creado);
    }

    @GetMapping("/{id}")
    public ClienteResponse porId(@PathVariable String id) {
        return servicio.porId(id);
    }
}
