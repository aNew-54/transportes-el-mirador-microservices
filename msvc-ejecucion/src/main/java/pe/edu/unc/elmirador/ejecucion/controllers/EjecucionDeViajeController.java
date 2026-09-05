package pe.edu.unc.elmirador.ejecucion.controllers;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.unc.elmirador.ejecucion.dto.request.CerrarEjecucionRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.ConformidadRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.CrearEjecucionRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.RegistrarCheckListRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.RegistrarEsperaRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.RegistrarIncidenciaRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.ReportarHitoRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.TransbordoRequest;
import pe.edu.unc.elmirador.ejecucion.dto.response.EjecucionDeViajeResponse;
import pe.edu.unc.elmirador.ejecucion.services.EjecucionDeViajeService;

@RestController
@RequestMapping("/api/v1/ejecuciones")
public class EjecucionDeViajeController {

    private final EjecucionDeViajeService servicio;

    public EjecucionDeViajeController(EjecucionDeViajeService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<EjecucionDeViajeResponse> crear(@Valid @RequestBody CrearEjecucionRequest peticion) {
        EjecucionDeViajeResponse creado = servicio.crear(peticion);
        return ResponseEntity.created(URI.create("/api/v1/ejecuciones/" + creado.viajeId())).body(creado);
    }

    @PostMapping("/{viajeId}/checklist")
    public ResponseEntity<EjecucionDeViajeResponse> registrarCheckList(
            @PathVariable String viajeId,
            @Valid @RequestBody RegistrarCheckListRequest peticion) {
        return ResponseEntity.ok(servicio.registrarCheckList(viajeId, peticion));
    }

    @PostMapping("/{viajeId}/iniciar")
    public ResponseEntity<EjecucionDeViajeResponse> iniciar(@PathVariable String viajeId) {
        return ResponseEntity.ok(servicio.iniciar(viajeId));
    }

    @PostMapping("/{viajeId}/hitos")
    public ResponseEntity<EjecucionDeViajeResponse> reportarHito(
            @PathVariable String viajeId,
            @Valid @RequestBody ReportarHitoRequest peticion) {
        // La doc dice que POST /ejecuciones/{viajeId}/hitos devuelve 201
        EjecucionDeViajeResponse respuesta = servicio.reportarHito(viajeId, peticion);
        return ResponseEntity.created(URI.create("/api/v1/ejecuciones/" + viajeId)).body(respuesta);
    }

    @PostMapping("/{viajeId}/incidencias")
    public ResponseEntity<EjecucionDeViajeResponse> registrarIncidencia(
            @PathVariable String viajeId,
            @Valid @RequestBody RegistrarIncidenciaRequest peticion) {
        // La doc dice que POST /ejecuciones/{viajeId}/incidencias devuelve 201
        EjecucionDeViajeResponse respuesta = servicio.registrarIncidencia(viajeId, peticion);
        return ResponseEntity.created(URI.create("/api/v1/ejecuciones/" + viajeId)).body(respuesta);
    }

    @PostMapping("/{viajeId}/transbordo")
    public ResponseEntity<EjecucionDeViajeResponse> transbordar(
            @PathVariable String viajeId,
            @Valid @RequestBody TransbordoRequest peticion) {
        return ResponseEntity.ok(servicio.transbordar(viajeId, peticion));
    }

    @PostMapping("/{viajeId}/paradas/{secuencia}/conformidad")
    public ResponseEntity<EjecucionDeViajeResponse> registrarConformidad(
            @PathVariable String viajeId,
            @PathVariable int secuencia,
            @Valid @RequestBody ConformidadRequest peticion) {
        // La doc dice que POST devuelve 201
        EjecucionDeViajeResponse respuesta = servicio.registrarConformidad(viajeId, secuencia, peticion);
        return ResponseEntity.created(URI.create("/api/v1/ejecuciones/" + viajeId)).body(respuesta);
    }

    @PostMapping("/{viajeId}/paradas/{secuencia}/espera")
    public ResponseEntity<EjecucionDeViajeResponse> registrarEspera(
            @PathVariable String viajeId,
            @PathVariable int secuencia,
            @Valid @RequestBody RegistrarEsperaRequest peticion) {
        return ResponseEntity.ok(servicio.registrarEspera(viajeId, secuencia, peticion));
    }

    @PostMapping("/{viajeId}/cerrar")
    public ResponseEntity<EjecucionDeViajeResponse> cerrar(
            @PathVariable String viajeId,
            @Valid @RequestBody CerrarEjecucionRequest peticion) {
        return ResponseEntity.ok(servicio.cerrar(viajeId, peticion));
    }

    @GetMapping("/{viajeId}")
    public ResponseEntity<EjecucionDeViajeResponse> obtener(@PathVariable String viajeId) {
        return ResponseEntity.ok(servicio.obtener(viajeId));
    }
}
