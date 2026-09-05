package pe.edu.unc.elmirador.facturacion.controllers;

import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.unc.elmirador.facturacion.dto.request.EmitirNotaDeCreditoRequest;
import pe.edu.unc.elmirador.facturacion.dto.response.NotaDeCreditoResponse;
import pe.edu.unc.elmirador.facturacion.services.NotaDeCreditoService;

@RestController
@RequestMapping("/api/v1")
public class NotaDeCreditoController {

    private final NotaDeCreditoService servicio;

    public NotaDeCreditoController(NotaDeCreditoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/notas-de-credito")
    public ResponseEntity<NotaDeCreditoResponse> emitir(
            @Valid @RequestBody EmitirNotaDeCreditoRequest peticion) {
        NotaDeCreditoResponse creada = servicio.emitir(peticion);
        return ResponseEntity.created(URI.create("/api/v1/notas-de-credito/" + creada.id())).body(creada);
    }
}
