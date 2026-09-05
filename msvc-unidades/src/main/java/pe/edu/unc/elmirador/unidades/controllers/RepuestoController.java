package pe.edu.unc.elmirador.unidades.controllers;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.unc.elmirador.unidades.dto.request.AjustarInventarioRequest;
import pe.edu.unc.elmirador.unidades.dto.request.RegistrarRepuestoRequest;
import pe.edu.unc.elmirador.unidades.dto.response.RepuestoResponse;
import pe.edu.unc.elmirador.unidades.services.RepuestoService;

@RestController
@RequestMapping("/api/v1/repuestos")
public class RepuestoController {

    private final RepuestoService servicio;

    public RepuestoController(RepuestoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<RepuestoResponse> registrar(
            @Valid @RequestBody RegistrarRepuestoRequest peticion) {
        RepuestoResponse creado = servicio.registrar(peticion);
        return ResponseEntity.created(URI.create("/api/v1/repuestos/" + creado.id())).body(creado);
    }

    @PostMapping("/{id}/movimientos")
    public RepuestoResponse ajustarInventario(
            @PathVariable String id,
            @Valid @RequestBody AjustarInventarioRequest peticion) {
        return servicio.ajustarInventario(id, peticion);
    }
}
