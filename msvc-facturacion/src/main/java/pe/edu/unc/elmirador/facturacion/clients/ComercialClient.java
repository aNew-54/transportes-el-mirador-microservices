package pe.edu.unc.elmirador.facturacion.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import pe.edu.unc.elmirador.facturacion.clients.dto.SnapshotFacturableRemoto;

@FeignClient(name = "comercial", url = "${clients.comercial.url}")
public interface ComercialClient {
    @GetMapping("/internal/v1/ordenes/{ordenId}/snapshot-facturable")
    SnapshotFacturableRemoto snapshotFacturableDe(@PathVariable("ordenId") String ordenId);
}
