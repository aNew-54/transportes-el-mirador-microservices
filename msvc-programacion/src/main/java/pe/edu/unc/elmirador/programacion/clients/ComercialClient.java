package pe.edu.unc.elmirador.programacion.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import pe.edu.unc.elmirador.programacion.clients.dto.OrdenRemota;

@FeignClient(name = "comercial", url = "${clients.comercial.url}")
public interface ComercialClient {
    @GetMapping("/internal/v1/ordenes/{ordenId}")
    OrdenRemota obtenerOrden(@PathVariable("ordenId") String ordenId);
}
