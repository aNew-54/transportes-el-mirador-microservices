package pe.edu.unc.elmirador.cobranza.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pe.edu.unc.elmirador.cobranza.dto.response.CarteraGestionResponse;
import pe.edu.unc.elmirador.cobranza.services.CuentaCorrienteService;

@RestController
@RequestMapping("/api/v1")
public class CarteraController {

    private final CuentaCorrienteService servicio;

    public CarteraController(CuentaCorrienteService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/cartera/gestion")
    public List<CarteraGestionResponse> gestion() {
        return servicio.carteraGestion();
    }
}
