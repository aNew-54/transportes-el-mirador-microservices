package pe.edu.unc.elmirador.programacion.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.unc.elmirador.programacion.dto.response.HojaDeRutaResponse;
import pe.edu.unc.elmirador.programacion.dto.response.ViajeResponse;
import pe.edu.unc.elmirador.programacion.exceptions.TransicionDeViajeInvalidaException;
import pe.edu.unc.elmirador.programacion.models.vo.EstadoDeViaje;
import pe.edu.unc.elmirador.programacion.services.ViajeService;

@RestController
@RequestMapping("/internal/v1/viajes")
public class ViajeInternalController {

    private final ViajeService servicio;

    public ViajeInternalController(ViajeService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/{id}/hoja-de-ruta")
    public ResponseEntity<HojaDeRutaResponse> consultarHojaDeRuta(@PathVariable String id) {
        ViajeResponse viaje = servicio.consultar(id);
        if (viaje.estado() == EstadoDeViaje.PLANIFICADO || viaje.hojaDeRuta() == null) {
            throw new TransicionDeViajeInvalidaException("El viaje " + id + " no tiene hoja de ruta emitida");
        }
        return ResponseEntity.ok(viaje.hojaDeRuta());
    }
}
