package pe.edu.unc.elmirador.programacion.controllers;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.unc.elmirador.programacion.dto.request.AsignarRecursosRequest;
import pe.edu.unc.elmirador.programacion.dto.request.ConsolidarOrdenRequest;
import pe.edu.unc.elmirador.programacion.dto.request.PlanificarViajeRequest;
import pe.edu.unc.elmirador.programacion.dto.request.ProgramarViajeRequest;
import pe.edu.unc.elmirador.programacion.dto.response.ViajeResponse;
import pe.edu.unc.elmirador.programacion.services.ViajeService;

@RestController
@RequestMapping("/api/v1/viajes")
public class ViajeController {

    private final ViajeService servicio;

    public ViajeController(ViajeService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<ViajeResponse> planificar(@Valid @RequestBody PlanificarViajeRequest peticion) {
        ViajeResponse creado = servicio.planificar(peticion);
        return ResponseEntity.created(URI.create("/api/v1/viajes/" + creado.id())).body(creado);
    }

    @PostMapping("/{id}/ordenes")
    public ResponseEntity<ViajeResponse> consolidarOrden(
            @PathVariable String id,
            @Valid @RequestBody ConsolidarOrdenRequest peticion) {
        ViajeResponse viaje = servicio.consolidarOrden(id, peticion);
        return ResponseEntity.ok(viaje);
    }

    @PostMapping("/{id}/recursos")
    public ResponseEntity<ViajeResponse> asignarRecursos(
            @PathVariable String id,
            @Valid @RequestBody AsignarRecursosRequest peticion) {
        ViajeResponse viaje = servicio.asignarRecursos(id, peticion);
        return ResponseEntity.ok(viaje);
    }

    @PostMapping("/{id}/programar")
    public ResponseEntity<ViajeResponse> programar(
            @PathVariable String id,
            @Valid @RequestBody ProgramarViajeRequest peticion) {
        ViajeResponse viaje = servicio.programar(id, peticion);
        return ResponseEntity.ok(viaje);
    }

    @PostMapping("/{id}/despachar")
    public ResponseEntity<ViajeResponse> despachar(@PathVariable String id) {
        ViajeResponse viaje = servicio.despachar(id);
        return ResponseEntity.ok(viaje);
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ViajeResponse> cancelar(@PathVariable String id) {
        ViajeResponse viaje = servicio.cancelar(id);
        return ResponseEntity.ok(viaje);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ViajeResponse> consultar(@PathVariable String id) {
        ViajeResponse viaje = servicio.consultar(id);
        return ResponseEntity.ok(viaje);
    }
}
