package pe.edu.unc.elmirador.comercial.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import pe.edu.unc.elmirador.comercial.clients.dto.EstadoCrediticioRemoto;

/**
 * Contrato 11 - Comercial consulta el estado crediticio a Cobranza.
 *
 * <p>Es la unica flecha saliente que el mapa de contexto le da a este modulo. Habla el idioma del
 * contrato y devuelve el DTO remoto sin tocarlo: la traduccion al dominio la hace
 * {@link CobranzaGateway}, que es tambien quien atrapa. Ningun servicio inyecta esta interfaz.
 *
 * <p>Los timeouts no se declaran aqui. Salen de {@code spring.cloud.openfeign.client.config.default}
 * en {@code application.properties}, con los valores que fija la regla 4 de {@code contracts.md}.
 */
@FeignClient(name = "cobranza", url = "${clients.cobranza.url}")
public interface CobranzaClient {

    @GetMapping("/internal/v1/clientes/{clienteId}/estado-crediticio")
    EstadoCrediticioRemoto estadoCrediticio(@PathVariable("clienteId") String clienteId);
}
