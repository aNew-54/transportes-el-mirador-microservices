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
import pe.edu.unc.elmirador.comercial.dto.request.CancelarOrdenRequest;
import pe.edu.unc.elmirador.comercial.dto.request.CrearOrdenRequest;
import pe.edu.unc.elmirador.comercial.dto.request.ReajustarCargaRequest;
import pe.edu.unc.elmirador.comercial.dto.response.OrdenDeServicioResponse;
import pe.edu.unc.elmirador.comercial.services.OrdenDeServicioService;

@RestController
@RequestMapping("/api/v1/ordenes")
public class OrdenDeServicioController {

    private final OrdenDeServicioService servicio;

    public OrdenDeServicioController(OrdenDeServicioService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<OrdenDeServicioResponse> crear(@Valid @RequestBody CrearOrdenRequest peticion) {
        OrdenDeServicioResponse creado = servicio.crear(peticion);
        return ResponseEntity
                .created(URI.create("/api/v1/ordenes/" + creado.id()))
                .body(creado);
    }

    @PostMapping("/{id}/confirmar")
    public OrdenDeServicioResponse confirmar(@PathVariable String id) {
        return servicio.confirmar(id);
    }

    @PostMapping("/{id}/cancelar")
    public OrdenDeServicioResponse cancelar(
            @PathVariable String id,
            @RequestBody(required = false) CancelarOrdenRequest peticion) {
        // Peticion opcional para soportar autorizacion de cancelacion post-despacho
        if (peticion == null) {
            peticion = new CancelarOrdenRequest(null);
        }
        return servicio.cancelar(id, peticion);
    }

    // Para uso interno de Ejecución
    @PostMapping("/{id}/diferencias-de-carga")
    public OrdenDeServicioResponse reajustarCarga(
            @PathVariable String id,
            @Valid @RequestBody ReajustarCargaRequest peticion) {
        return servicio.reajustarCarga(id, peticion);
    }
    
    // Contratos internos - de solo lectura
    @GetMapping("/{id}")
    public OrdenDeServicioResponse porId(@PathVariable String id) {
        return servicio.porId(id);
    }
    
    @GetMapping("/{id}/snapshot-facturable")
    public OrdenDeServicioResponse snapshotFacturable(@PathVariable String id) {
        return servicio.porId(id);
    }
}
