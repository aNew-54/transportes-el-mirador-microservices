package pe.edu.unc.elmirador.ejecucion.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import pe.edu.unc.elmirador.ejecucion.clients.dto.HorasConduccionPeticion;
import pe.edu.unc.elmirador.ejecucion.clients.dto.IncidenciaPeticion;

@FeignClient(name = "conductores", url = "${clients.conductores.url}")
public interface ConductoresClient {

    @PostMapping("/internal/v1/conductores/{conductorId}/horas-conduccion")
    void reportarHoras(@PathVariable("conductorId") String conductorId, @RequestHeader("Idempotency-Key") String clave, @RequestBody HorasConduccionPeticion peticion);

    @PostMapping("/internal/v1/conductores/{conductorId}/incidencias")
    void reportarIncidencia(@PathVariable("conductorId") String conductorId, @RequestHeader("Idempotency-Key") String clave, @RequestBody IncidenciaPeticion peticion);
}
