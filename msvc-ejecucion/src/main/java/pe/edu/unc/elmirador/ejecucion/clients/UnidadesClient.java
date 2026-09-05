package pe.edu.unc.elmirador.ejecucion.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import pe.edu.unc.elmirador.ejecucion.clients.dto.FallaPeticion;
import pe.edu.unc.elmirador.ejecucion.clients.dto.KilometrajePeticion;

@FeignClient(name = "unidades", url = "${clients.unidades.url}")
public interface UnidadesClient {

    @PostMapping("/internal/v1/unidades/{unidadId}/kilometraje")
    void reportarKilometraje(@PathVariable("unidadId") String unidadId, @RequestHeader("Idempotency-Key") String clave, @RequestBody KilometrajePeticion peticion);

    @PostMapping("/internal/v1/unidades/{unidadId}/fallas")
    void reportarFalla(@PathVariable("unidadId") String unidadId, @RequestHeader("Idempotency-Key") String clave, @RequestBody FallaPeticion peticion);
}
