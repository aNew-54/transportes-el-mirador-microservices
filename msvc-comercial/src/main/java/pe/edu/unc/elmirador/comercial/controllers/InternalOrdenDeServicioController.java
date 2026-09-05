package pe.edu.unc.elmirador.comercial.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.unc.elmirador.comercial.dto.request.ReajustarCargaRequest;
import pe.edu.unc.elmirador.comercial.dto.response.OrdenDeServicioResponse;
import pe.edu.unc.elmirador.comercial.services.OrdenDeServicioService;

@RestController
@RequestMapping("/internal/v1/ordenes")
public class InternalOrdenDeServicioController {

    private final OrdenDeServicioService servicio;

    public InternalOrdenDeServicioController(OrdenDeServicioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/{ordenId}")
    public OrdenDeServicioResponse getOrden(@PathVariable String ordenId) {
        return servicio.porId(ordenId);
    }

    @PostMapping("/{ordenId}/diferencias-de-carga")
    public OrdenDeServicioResponse reajustarCarga(
            @PathVariable String ordenId,
            @Valid @RequestBody ReajustarCargaRequest peticion) {
        return servicio.reajustarCarga(ordenId, peticion);
    }

    // El contrato 7 tambien incluye /esperas, pero el dominio actual de Comercial no expone una invariante para esperas
    // (probablemente eso vaya en otro servicio o se reciba sin validacion de negocio).
    // Lo agrego vacio o basico si es estrictamente necesario, pero la spec no da cuerpo.
    // Solo expongo lo listado explícitamente en la spec.

    @GetMapping("/{ordenId}/snapshot-facturable")
    public OrdenDeServicioResponse snapshotFacturable(@PathVariable String ordenId) {
        return servicio.porId(ordenId);
    }
}
