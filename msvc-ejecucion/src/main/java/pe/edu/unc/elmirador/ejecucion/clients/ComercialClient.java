package pe.edu.unc.elmirador.ejecucion.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import pe.edu.unc.elmirador.ejecucion.clients.dto.DiferenciaCargaPeticion;
import pe.edu.unc.elmirador.ejecucion.clients.dto.EsperaPeticion;

@FeignClient(name = "comercial", url = "${clients.comercial.url}")
public interface ComercialClient {

    @PostMapping("/internal/v1/ordenes/{ordenId}/diferencias-de-carga")
    void reportarDiferencia(@PathVariable("ordenId") String ordenId, @RequestHeader("Idempotency-Key") String clave, @RequestBody DiferenciaCargaPeticion peticion);

    @PostMapping("/internal/v1/ordenes/{ordenId}/esperas")
    void reportarEspera(@PathVariable("ordenId") String ordenId, @RequestHeader("Idempotency-Key") String clave, @RequestBody EsperaPeticion peticion);
}
