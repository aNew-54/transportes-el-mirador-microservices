package pe.edu.unc.elmirador.ejecucion.controllers;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.unc.elmirador.ejecucion.dto.request.AbrirLiquidacionRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.RendirGastoRequest;
import pe.edu.unc.elmirador.ejecucion.dto.response.LiquidacionDeViajeResponse;
import pe.edu.unc.elmirador.ejecucion.services.LiquidacionDeViajeService;

@RestController
@RequestMapping("/api/v1/liquidaciones")
public class LiquidacionDeViajeController {

    private final LiquidacionDeViajeService servicio;

    public LiquidacionDeViajeController(LiquidacionDeViajeService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<LiquidacionDeViajeResponse> abrir(@Valid @RequestBody AbrirLiquidacionRequest peticion) {
        LiquidacionDeViajeResponse creado = servicio.abrir(peticion);
        return ResponseEntity.created(URI.create("/api/v1/liquidaciones/" + creado.viajeId() + "/" + creado.conductorId()))
                .body(creado);
    }

    @PostMapping("/{viajeId}/{conductorId}/gastos")
    public ResponseEntity<LiquidacionDeViajeResponse> rendirGasto(
            @PathVariable String viajeId,
            @PathVariable String conductorId,
            @Valid @RequestBody RendirGastoRequest peticion) {
        LiquidacionDeViajeResponse respuesta = servicio.rendirGasto(viajeId, conductorId, peticion);
        return ResponseEntity.created(URI.create("/api/v1/liquidaciones/" + viajeId + "/" + conductorId))
                .body(respuesta);
    }

    @PostMapping("/{viajeId}/{conductorId}/aprobar")
    public ResponseEntity<LiquidacionDeViajeResponse> aprobar(
            @PathVariable String viajeId,
            @PathVariable String conductorId) {
        return ResponseEntity.ok(servicio.aprobar(viajeId, conductorId));
    }
}
