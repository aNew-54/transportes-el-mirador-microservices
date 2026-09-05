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
import pe.edu.unc.elmirador.unidades.dto.request.MotivoRequest;
import pe.edu.unc.elmirador.unidades.dto.request.RegistrarDocumentoRequest;
import pe.edu.unc.elmirador.unidades.dto.request.RegistrarUnidadRequest;
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

    @PostMapping("/api/v1/unidades/{id}/inoperativa")
    public UnidadResponse marcarInoperativa(
            @PathVariable String id,
            @Valid @RequestBody MotivoRequest peticion) {
        return servicio.marcarInoperativa(id, peticion);
    }

    @PostMapping("/api/v1/unidades/{id}/taller")
    public UnidadResponse marcarEnTaller(
            @PathVariable String id,
            @Valid @RequestBody MotivoRequest peticion) {
        return servicio.marcarEnTaller(id, peticion);
    }

    @PostMapping("/api/v1/unidades/{id}/reactivar")
    public UnidadResponse reactivar(@PathVariable String id) {
        return servicio.reactivar(id);
    }

}
