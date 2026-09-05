package pe.edu.unc.elmirador.unidades.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unc.elmirador.unidades.dto.response.AlertaResponse;
import pe.edu.unc.elmirador.unidades.services.UnidadService;

@RestController
@RequestMapping("/api/v1/alertas")
public class AlertaController {

    private final UnidadService servicio;

    public AlertaController(UnidadService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<AlertaResponse> alertas() {
        return servicio.alertas();
    }
}
