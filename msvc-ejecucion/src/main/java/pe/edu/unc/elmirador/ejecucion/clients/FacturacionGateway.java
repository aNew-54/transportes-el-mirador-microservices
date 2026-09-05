package pe.edu.unc.elmirador.ejecucion.clients;

import org.springframework.stereotype.Component;

import feign.FeignException;
import feign.RetryableException;
import pe.edu.unc.elmirador.ejecucion.clients.dto.ConformidadPeticion;
import pe.edu.unc.elmirador.ejecucion.exceptions.FacturacionIntegrationException;

@Component
public class FacturacionGateway {

    private final FacturacionClient cliente;

    public FacturacionGateway(FacturacionClient cliente) {
        this.cliente = cliente;
    }

    public void registrarConformidad(ConformidadPeticion peticion) {
        String clave = claveConformidad(peticion.viajeId(), peticion.ordenDeServicioId());
        try {
            cliente.registrarConformidad(clave, peticion);
        } catch (RetryableException fallo) {
            throw new FacturacionIntegrationException(
                    "Facturacion no respondio al registrar conformidad para el viaje " + peticion.viajeId(), fallo);
        } catch (FeignException fallo) {
            throw new FacturacionIntegrationException(
                    "Facturacion respondio " + fallo.status() + " al registrar conformidad para el viaje " + peticion.viajeId() + ": " + fallo.contentUTF8(), fallo);
        }
    }

    String claveConformidad(String viajeId, String ordenId) {
        return viajeId + ":" + ordenId + ":conformidad";
    }
}
