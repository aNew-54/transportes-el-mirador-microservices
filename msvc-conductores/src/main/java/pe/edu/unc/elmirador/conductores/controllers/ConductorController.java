package pe.edu.unc.elmirador.conductores.controllers;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import pe.edu.unc.elmirador.conductores.dto.request.RegistrarConductorRequest;
import pe.edu.unc.elmirador.conductores.dto.request.RegistrarInduccionRequest;
import pe.edu.unc.elmirador.conductores.dto.request.RenovarLicenciaRequest;
import pe.edu.unc.elmirador.conductores.dto.request.SuspenderConductorRequest;
import pe.edu.unc.elmirador.conductores.dto.response.AlertaResponse;
import pe.edu.unc.elmirador.conductores.dto.response.ConductorResponse;
import pe.edu.unc.elmirador.conductores.dto.response.HorasResponse;
import pe.edu.unc.elmirador.conductores.dto.response.InduccionResponse;
import pe.edu.unc.elmirador.conductores.models.vo.SituacionDeHabilitacion;
import pe.edu.unc.elmirador.conductores.services.ConductorService;

/**
 * API publica del contexto (regla 4: {@code /api/v1}).
 *
 * <p>No atrapa nada y no decide nada. Todo fallo sube y lo traduce {@link ManejadorDeErrores}, que es
 * el unico punto del modulo que conoce codigos HTTP.
 */
@RestController
@RequestMapping("/api/v1")
public class ConductorController {

    private final ConductorService servicio;

    public ConductorController(ConductorService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/conductores")
    public ResponseEntity<ConductorResponse> registrar(
            @Valid @RequestBody RegistrarConductorRequest peticion) {
        ConductorResponse creado = servicio.registrar(peticion);
        return ResponseEntity
                .created(URI.create("/api/v1/conductores/" + creado.id()))
                .body(creado);
    }

    @GetMapping("/conductores/{id}")
    public ConductorResponse porId(@PathVariable String id) {
        return servicio.porId(id);
    }

    @GetMapping("/conductores")
    public List<ConductorResponse> listar(
            @RequestParam(required = false) SituacionDeHabilitacion situacion) {
        return servicio.listar(situacion);
    }

    @PostMapping("/conductores/{id}/licencia")
    public ConductorResponse renovarLicencia(
            @PathVariable String id,
            @Valid @RequestBody RenovarLicenciaRequest peticion) {
        return servicio.renovarLicencia(id, peticion);
    }

    @PostMapping("/conductores/{id}/inducciones")
    public ResponseEntity<InduccionResponse> registrarInduccion(
            @PathVariable String id,
            @Valid @RequestBody RegistrarInduccionRequest peticion) {
        InduccionResponse creada = servicio.registrarInduccion(id, peticion);
        return ResponseEntity
                .created(URI.create("/api/v1/conductores/" + id + "/inducciones/" + creada.id()))
                .body(creada);
    }

    @GetMapping("/conductores/{id}/horas")
    public HorasResponse horas(@PathVariable String id) {
        return servicio.horas(id);
    }

    @PostMapping("/conductores/{id}/descanso")
    public ConductorResponse registrarDescanso(@PathVariable String id) {
        return servicio.registrarDescanso(id);
    }

    @PostMapping("/conductores/{id}/suspender")
    public ConductorResponse suspender(
            @PathVariable String id,
            @Valid @RequestBody SuspenderConductorRequest peticion) {
        return servicio.suspender(id, peticion);
    }

    @PostMapping("/conductores/{id}/rehabilitar")
    public ConductorResponse rehabilitar(@PathVariable String id) {
        return servicio.rehabilitar(id);
    }

    @GetMapping("/alertas")
    public List<AlertaResponse> alertas(@RequestParam(defaultValue = "30") int dias) {
        return servicio.alertas(dias);
    }
}
