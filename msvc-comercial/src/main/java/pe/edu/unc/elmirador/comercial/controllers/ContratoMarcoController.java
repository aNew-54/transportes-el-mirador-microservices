package pe.edu.unc.elmirador.comercial.controllers;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.unc.elmirador.comercial.dto.request.RegistrarContratoMarcoRequest;
import pe.edu.unc.elmirador.comercial.dto.response.ContratoMarcoResponse;
import pe.edu.unc.elmirador.comercial.services.ContratoMarcoService;

@RestController
@RequestMapping("/api/v1/contratos-marco")
public class ContratoMarcoController {

    private final ContratoMarcoService servicio;

    public ContratoMarcoController(ContratoMarcoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<ContratoMarcoResponse> registrar(@Valid @RequestBody RegistrarContratoMarcoRequest peticion) {
        ContratoMarcoResponse creado = servicio.registrar(peticion);
        return ResponseEntity
                .created(URI.create("/api/v1/contratos-marco/" + creado.id()))
                .body(creado);
    }
}
