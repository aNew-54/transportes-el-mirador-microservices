package pe.edu.unc.elmirador.comercial.controllers;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.unc.elmirador.comercial.dto.request.RegistrarTarifarioRequest;
import pe.edu.unc.elmirador.comercial.dto.response.TarifarioResponse;
import pe.edu.unc.elmirador.comercial.services.TarifarioService;

@RestController
@RequestMapping("/api/v1/tarifarios")
public class TarifarioController {

    private final TarifarioService servicio;

    public TarifarioController(TarifarioService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<TarifarioResponse> publicar(@Valid @RequestBody RegistrarTarifarioRequest peticion) {
        TarifarioResponse creado = servicio.publicar(peticion);
        return ResponseEntity
                .created(URI.create("/api/v1/tarifarios/" + creado.id()))
                .body(creado);
    }
}
