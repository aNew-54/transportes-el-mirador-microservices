package pe.edu.unc.elmirador.comercial.clients;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import feign.FeignException;
import feign.RetryableException;
import pe.edu.unc.elmirador.comercial.clients.dto.EstadoCrediticioRemoto;
import pe.edu.unc.elmirador.comercial.exceptions.CobranzaIntegrationException;
import pe.edu.unc.elmirador.comercial.models.vo.EstadoCrediticio;
import pe.edu.unc.elmirador.comercial.models.vo.SituacionCrediticia;

/**
 * Capa anticorrupcion del contrato 11. Es el unico punto de Comercial que conoce la forma de Cobranza.
 *
 * <p>Traduce en los dos sentidos que importan: el DTO remoto se convierte en el objeto de valor propio
 * {@link EstadoCrediticio}, y cualquier fallo remoto se convierte en
 * {@link CobranzaIntegrationException}. Hacia arriba no se escapa nada de Feign.
 */
@Component
public class CobranzaGateway {

    private final CobranzaClient cliente;

    public CobranzaGateway(CobranzaClient cliente) {
        this.cliente = cliente;
    }

    /**
     * Contrato 11. Devuelve el estado crediticio vigente segun Cobranza, que es la fuente de verdad.
     *
     * @throws CobranzaIntegrationException si Cobranza no responde, responde un error o responde algo
     *     que este modulo no sabe leer. Nunca devuelve un valor por defecto: el contrato 11 prohibe
     *     expresamente asumir {@code VIGENTE} ante el silencio.
     */
    public EstadoCrediticio estadoCrediticioDe(String clienteId) {
        EstadoCrediticioRemoto remoto;
        try {
            remoto = cliente.estadoCrediticio(clienteId);
        } catch (RetryableException fallo) {
            // El socket no abrio o vencio el read-timeout. NO hereda de FeignException: si solo se
            // atrapara aquella, el unico caso que este slice existe para cubrir se escaparia sin traducir.
            throw new CobranzaIntegrationException(
                    "Cobranza no respondio al consultar el estado crediticio del cliente " + clienteId, fallo);
        } catch (FeignException fallo) {
            // Incluye el 404. Regla 5: un 404 remoto no es «el cliente no existe», es que Cobranza y
            // Comercial discrepan sobre que clientes hay.
            throw new CobranzaIntegrationException(
                    "Cobranza respondio " + fallo.status() + " al consultar el estado crediticio del cliente "
                            + clienteId + ": " + fallo.contentUTF8(), fallo);
        }
        return traducir(clienteId, remoto);
    }

    /**
     * Un cuerpo que no se entiende tambien es un fallo de integracion, no un 500 de este modulo. Si
     * Cobranza anade manana una tercera situacion, {@code valueOf} lanzaria una excepcion que culparia
     * a Comercial de un cambio ajeno.
     */
    private EstadoCrediticio traducir(String clienteId, EstadoCrediticioRemoto remoto) {
        if (remoto == null || remoto.situacion() == null || remoto.fechaDeCambio() == null) {
            throw new CobranzaIntegrationException(
                    "Cobranza respondio un estado crediticio incompleto para el cliente " + clienteId);
        }
        SituacionCrediticia situacion;
        try {
            situacion = SituacionCrediticia.valueOf(remoto.situacion());
        } catch (IllegalArgumentException desconocida) {
            throw new CobranzaIntegrationException(
                    "Cobranza respondio una situacion crediticia que Comercial no conoce: "
                            + remoto.situacion(), desconocida);
        }
        LocalDate fecha = remoto.fechaDeCambio();
        return new EstadoCrediticio(situacion, fecha);
    }
}
