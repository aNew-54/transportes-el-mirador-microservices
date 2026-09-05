package pe.edu.unc.elmirador.unidades.controllers;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.unc.elmirador.unidades.dto.request.AbrirOrdenRequest;
import pe.edu.unc.elmirador.unidades.dto.request.RegistrarTrabajoRequest;
import pe.edu.unc.elmirador.unidades.dto.response.OrdenDeMantenimientoResponse;
import pe.edu.unc.elmirador.unidades.services.OrdenDeMantenimientoService;

@RestController
@RequestMapping("/api/v1/ordenes-mantenimiento")
public class OrdenDeMantenimientoController {

    private final OrdenDeMantenimientoService servicio;

    public OrdenDeMantenimientoController(OrdenDeMantenimientoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<OrdenDeMantenimientoResponse> abrir(
            @Valid @RequestBody AbrirOrdenRequest peticion) {
        OrdenDeMantenimientoResponse creada = servicio.abrir(peticion);
        return ResponseEntity.created(URI.create("/api/v1/ordenes-mantenimiento/" + creada.id())).body(creada);
    }

    @PostMapping("/{id}/trabajos")
    public ResponseEntity<OrdenDeMantenimientoResponse> registrarTrabajo(
            @PathVariable String id,
            @Valid @RequestBody RegistrarTrabajoRequest peticion) {
        OrdenDeMantenimientoResponse actualizada = servicio.registrarTrabajo(id, peticion);
        return ResponseEntity.created(URI.create("/api/v1/ordenes-mantenimiento/" + id + "/trabajos")).body(actualizada);
    }

    @PostMapping("/{id}/cerrar")
    public OrdenDeMantenimientoResponse cerrar(@PathVariable String id) {
        return servicio.cerrar(id);
    }
}
