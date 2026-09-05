package pe.edu.unc.elmirador.ejecucion.clients;

import org.springframework.stereotype.Component;

import feign.FeignException;
import feign.RetryableException;
import pe.edu.unc.elmirador.ejecucion.clients.dto.FallaPeticion;
import pe.edu.unc.elmirador.ejecucion.clients.dto.KilometrajePeticion;
import pe.edu.unc.elmirador.ejecucion.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.ejecucion.exceptions.UnidadesIntegrationException;

@Component
public class UnidadesGateway {

    private final UnidadesClient cliente;

    public UnidadesGateway(UnidadesClient cliente) {
        this.cliente = cliente;
    }

    public void reportarKilometraje(String unidadId, KilometrajePeticion peticion) {
        String clave = claveKilometraje(peticion.viajeId());
        try {
            cliente.reportarKilometraje(unidadId, clave, peticion);
        } catch (RetryableException fallo) {
            throw new UnidadesIntegrationException(
                    "Unidades no respondio al reportar el kilometraje para la unidad " + unidadId, fallo);
        } catch (FeignException.Conflict rechazo) {
            // UNI-03: el kilometraje es menor al vigente. Es «asi no», no «no pude comprobarlo».
            throw new ConflictoDeRecursoException(
                    "Unidades rechazo el kilometraje " + peticion.kilometraje() + " para la unidad "
                            + unidadId + ": es menor al vigente (UNI-03)");
        } catch (FeignException fallo) {
            throw new UnidadesIntegrationException(
                    "Unidades respondio " + fallo.status() + " al reportar el kilometraje para la unidad " + unidadId + ": " + fallo.contentUTF8(), fallo);
        }
    }

    public void reportarFalla(String unidadId, String fallaId, FallaPeticion peticion) {
        String clave = claveFalla(peticion.viajeId(), fallaId);
        try {
            cliente.reportarFalla(unidadId, clave, peticion);
        } catch (RetryableException fallo) {
            throw new UnidadesIntegrationException(
                    "Unidades no respondio al reportar la falla para la unidad " + unidadId, fallo);
        } catch (FeignException fallo) {
            throw new UnidadesIntegrationException(
                    "Unidades respondio " + fallo.status() + " al reportar la falla para la unidad " + unidadId + ": " + fallo.contentUTF8(), fallo);
        }
    }

    String claveKilometraje(String viajeId) {
        return viajeId + ":km-final";
    }

    String claveFalla(String viajeId, String fallaId) {
        return viajeId + ":falla:" + fallaId;
    }
}
