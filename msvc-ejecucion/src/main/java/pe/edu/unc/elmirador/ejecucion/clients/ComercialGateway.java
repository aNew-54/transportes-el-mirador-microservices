package pe.edu.unc.elmirador.ejecucion.clients;

import org.springframework.stereotype.Component;

import feign.FeignException;
import feign.RetryableException;
import pe.edu.unc.elmirador.ejecucion.clients.dto.DiferenciaCargaPeticion;
import pe.edu.unc.elmirador.ejecucion.clients.dto.EsperaPeticion;
import pe.edu.unc.elmirador.ejecucion.exceptions.ComercialIntegrationException;

@Component
public class ComercialGateway {

    private final ComercialClient cliente;

    public ComercialGateway(ComercialClient cliente) {
        this.cliente = cliente;
    }

    public void reportarDiferencia(String ordenId, DiferenciaCargaPeticion peticion) {
        String clave = claveDiferencia(peticion.viajeId(), ordenId);
        try {
            cliente.reportarDiferencia(ordenId, clave, peticion);
        } catch (RetryableException fallo) {
            throw new ComercialIntegrationException(
                    "Comercial no respondio al reportar diferencia de carga para la orden " + ordenId, fallo);
        } catch (FeignException fallo) {
            throw new ComercialIntegrationException(
                    "Comercial respondio " + fallo.status() + " al reportar diferencia de carga para la orden " + ordenId, fallo);
        }
    }

    public void reportarEspera(String ordenId, EsperaPeticion peticion) {
        String clave = claveEspera(peticion.viajeId(), ordenId, peticion.punto());
        try {
            cliente.reportarEspera(ordenId, clave, peticion);
        } catch (RetryableException fallo) {
            throw new ComercialIntegrationException(
                    "Comercial no respondio al reportar espera para la orden " + ordenId, fallo);
        } catch (FeignException fallo) {
            throw new ComercialIntegrationException(
                    "Comercial respondio " + fallo.status() + " al reportar espera para la orden " + ordenId, fallo);
        }
    }

    String claveDiferencia(String viajeId, String ordenId) {
        return viajeId + ":" + ordenId + ":diferencia";
    }

    String claveEspera(String viajeId, String ordenId, String punto) {
        return viajeId + ":" + ordenId + ":espera:" + punto;
    }
}
