package pe.edu.unc.elmirador.programacion.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.OffsetDateTime;
import pe.edu.unc.elmirador.programacion.clients.dto.ElegibilidadConductorRemota;

@FeignClient(name = "conductores", url = "${clients.conductores.url}")
public interface ConductoresClient {
    @GetMapping("/internal/v1/conductores/{conductorId}/elegibilidad")
    ElegibilidadConductorRemota consultarElegibilidad(
        @PathVariable("conductorId") String conductorId,
        @RequestParam("desde") OffsetDateTime desde,
        @RequestParam("hasta") OffsetDateTime hasta,
        @RequestParam("tipoUnidad") String tipoUnidad,
        @RequestParam(value = "clienteId", required = false) String clienteId
    );
}
