package pe.edu.unc.elmirador.programacion.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.unc.elmirador.programacion.dto.internal.response.HojaDeRutaContratoResponse;
import pe.edu.unc.elmirador.programacion.services.ViajeInternalService;

@RestController
@RequestMapping("/internal/v1/viajes")
public class ViajeInternalController {

    private final ViajeInternalService servicio;

    public ViajeInternalController(ViajeInternalService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/{viajeId}/hoja-de-ruta")
    public ResponseEntity<HojaDeRutaContratoResponse> obtenerHojaDeRuta(@PathVariable String viajeId) {
        HojaDeRutaContratoResponse respuesta = servicio.obtenerHojaDeRutaEjecutable(viajeId);
        return ResponseEntity.status(HttpStatus.OK).body(respuesta);
    }
}
