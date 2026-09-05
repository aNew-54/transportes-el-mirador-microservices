package pe.edu.unc.elmirador.unidades.controllers;

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
import pe.edu.unc.elmirador.unidades.dto.request.ActualizarKilometrajeRequest;
import pe.edu.unc.elmirador.unidades.dto.request.CambiarEstadoRequest;
import pe.edu.unc.elmirador.unidades.dto.request.RegistrarDocumentoRequest;
import pe.edu.unc.elmirador.unidades.dto.request.RegistrarFallaRequest;
import pe.edu.unc.elmirador.unidades.dto.request.RegistrarUnidadRequest;
import pe.edu.unc.elmirador.unidades.dto.response.ElegibilidadResponse;
import pe.edu.unc.elmirador.unidades.dto.response.UnidadResponse;
import pe.edu.unc.elmirador.unidades.models.vo.SituacionOperativa;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeCarga;
import pe.edu.unc.elmirador.unidades.services.UnidadService;

@RestController
public class UnidadController {

    private final UnidadService servicio;

    public UnidadController(UnidadService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/api/v1/unidades")
    public ResponseEntity<UnidadResponse> registrar(@Valid @RequestBody RegistrarUnidadRequest peticion) {
        UnidadResponse creada = servicio.registrar(peticion);
        return ResponseEntity.created(URI.create("/api/v1/unidades/" + creada.id())).body(creada);
    }

    @GetMapping("/api/v1/unidades/{id}")
    public UnidadResponse porId(@PathVariable String id) {
        return servicio.porId(id);
    }

    @GetMapping("/api/v1/unidades")
    public List<UnidadResponse> listar(@RequestParam(required = false) SituacionOperativa situacion) {
        return servicio.listar(situacion);
    }

    @PostMapping("/api/v1/unidades/{id}/documentos")
    public ResponseEntity<UnidadResponse> registrarDocumento(
            @PathVariable String id,
            @Valid @RequestBody RegistrarDocumentoRequest peticion) {
        UnidadResponse actualizada = servicio.registrarDocumento(id, peticion);
        return ResponseEntity.created(URI.create("/api/v1/unidades/" + id + "/documentos")).body(actualizada);
    }

    @PostMapping("/api/v1/unidades/{id}/estado")
    public UnidadResponse cambiarEstado(
            @PathVariable String id,
            @Valid @RequestBody CambiarEstadoRequest peticion) {
        return servicio.cambiarEstado(id, peticion);
    }

    // INTERNAL API

    @GetMapping("/internal/v1/unidades/{id}/elegibilidad")
    public ElegibilidadResponse elegibilidad(
            @PathVariable String id,
            @RequestParam int pesoKg,
            @RequestParam java.math.BigDecimal volumenM3,
            @RequestParam(required = false) TipoDeCarga carga) {
        return servicio.verificarElegibilidad(id, pesoKg, volumenM3, carga);
    }

    @PostMapping("/internal/v1/unidades/{id}/kilometraje")
    public void actualizarKilometraje(
            @PathVariable String id,
            @Valid @RequestBody ActualizarKilometrajeRequest peticion) {
        servicio.actualizarKilometraje(id, peticion);
    }

    @PostMapping("/internal/v1/unidades/{id}/fallas")
    public void registrarFalla(
            @PathVariable String id,
            @Valid @RequestBody RegistrarFallaRequest peticion) {
        servicio.registrarFalla(id, peticion);
    }
}
