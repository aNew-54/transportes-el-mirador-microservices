package pe.edu.unc.elmirador.ejecucion.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import pe.edu.unc.elmirador.ejecucion.clients.dto.ConformidadPeticion;

@FeignClient(name = "facturacion", url = "${clients.facturacion.url}")
public interface FacturacionClient {

    @PostMapping("/internal/v1/conformidades")
    void registrarConformidad(@RequestHeader("Idempotency-Key") String clave, @RequestBody ConformidadPeticion peticion);
}
