package pe.edu.unc.elmirador.programacion.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.unc.elmirador.programacion.dto.response.AgendaDeConductorResponse;
import pe.edu.unc.elmirador.programacion.dto.response.AgendaDeUnidadResponse;
import pe.edu.unc.elmirador.programacion.services.AgendaService;

@RestController
@RequestMapping("/api/v1/agendas")
public class AgendaController {

    private final AgendaService servicio;

    public AgendaController(AgendaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/unidades/{unidadId}")
    public ResponseEntity<AgendaDeUnidadResponse> consultarAgendaDeUnidad(@PathVariable String unidadId) {
        AgendaDeUnidadResponse agenda = servicio.consultarAgendaDeUnidad(unidadId);
        return ResponseEntity.ok(agenda);
    }

    @GetMapping("/conductores/{conductorId}")
    public ResponseEntity<AgendaDeConductorResponse> consultarAgendaDeConductor(@PathVariable String conductorId) {
        AgendaDeConductorResponse agenda = servicio.consultarAgendaDeConductor(conductorId);
        return ResponseEntity.ok(agenda);
    }
}
