package pe.edu.unc.elmirador.ejecucion.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import pe.edu.unc.elmirador.ejecucion.clients.dto.HojaDeRutaRemota;

@FeignClient(name = "programacion", url = "${clients.programacion.url}")
public interface ProgramacionClient {

    @GetMapping("/internal/v1/viajes/{viajeId}/hoja-de-ruta")
    HojaDeRutaRemota obtenerHojaDeRuta(@PathVariable("viajeId") String viajeId);
}
