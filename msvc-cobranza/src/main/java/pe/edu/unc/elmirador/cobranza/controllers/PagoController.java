package pe.edu.unc.elmirador.cobranza.controllers;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.unc.elmirador.cobranza.dto.request.AplicarPagoRequest;
import pe.edu.unc.elmirador.cobranza.dto.request.RegistrarPagoRequest;
import pe.edu.unc.elmirador.cobranza.dto.response.PagoResponse;
import pe.edu.unc.elmirador.cobranza.services.PagoService;

@RestController
@RequestMapping("/api/v1")
public class PagoController {

    private final PagoService servicio;

    public PagoController(PagoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/pagos")
    public ResponseEntity<PagoResponse> registrar(@Valid @RequestBody RegistrarPagoRequest peticion) {
        PagoResponse respuesta = servicio.registrar(peticion);
        return ResponseEntity.created(URI.create("/api/v1/pagos/" + respuesta.id())).body(respuesta);
    }

    @PostMapping("/pagos/{id}/aplicaciones")
    public ResponseEntity<PagoResponse> aplicar(
            @PathVariable String id,
            @Valid @RequestBody AplicarPagoRequest peticion) {
        PagoResponse respuesta = servicio.aplicar(id, peticion);
        return ResponseEntity.created(URI.create("/api/v1/pagos/" + id + "/aplicaciones")).body(respuesta);
    }
}
