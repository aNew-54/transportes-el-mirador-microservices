package pe.edu.unc.elmirador.facturacion.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import pe.edu.unc.elmirador.facturacion.clients.dto.CuentaPorCobrarCreadaRemoto;
import pe.edu.unc.elmirador.facturacion.clients.dto.CuentaPorCobrarRequestRemoto;

@FeignClient(name = "cobranza", url = "${clients.cobranza.url}")
public interface CobranzaClient {
    @PostMapping("/internal/v1/cuentas-por-cobrar")
    ResponseEntity<CuentaPorCobrarCreadaRemoto> crearCuentaPorCobrar(
        @RequestHeader("Idempotency-Key") String clave,
        @RequestBody CuentaPorCobrarRequestRemoto peticion
    );
}
