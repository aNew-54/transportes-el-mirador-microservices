package pe.edu.unc.elmirador.comercial.controllers;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.unc.elmirador.comercial.dto.request.AceptarCotizacionRequest;
import pe.edu.unc.elmirador.comercial.dto.request.EmitirCotizacionRequest;
import pe.edu.unc.elmirador.comercial.dto.request.RechazarCotizacionRequest;
import pe.edu.unc.elmirador.comercial.dto.response.CotizacionResponse;
import pe.edu.unc.elmirador.comercial.services.CotizacionService;

@RestController
@RequestMapping("/api/v1/cotizaciones")
public class CotizacionController {

    private final CotizacionService servicio;

    public CotizacionController(CotizacionService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<CotizacionResponse> emitir(@Valid @RequestBody EmitirCotizacionRequest peticion) {
        CotizacionResponse creado = servicio.emitir(peticion);
        return ResponseEntity
                .created(URI.create("/api/v1/cotizaciones/" + creado.id()))
                .body(creado);
    }

    @PostMapping("/{id}/aceptar")
    public CotizacionResponse aceptar(
            @PathVariable String id, 
            @Valid @RequestBody AceptarCotizacionRequest peticion) {
        return servicio.aceptar(id, peticion);
    }

    @PostMapping("/{id}/rechazar")
    public CotizacionResponse rechazar(
            @PathVariable String id, 
            @Valid @RequestBody RechazarCotizacionRequest peticion) {
        return servicio.rechazar(id, peticion);
    }
}
