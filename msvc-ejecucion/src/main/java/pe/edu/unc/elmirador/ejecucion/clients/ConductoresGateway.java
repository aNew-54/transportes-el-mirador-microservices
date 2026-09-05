package pe.edu.unc.elmirador.ejecucion.clients;

import org.springframework.stereotype.Component;

import feign.FeignException;
import feign.RetryableException;
import pe.edu.unc.elmirador.ejecucion.clients.dto.HorasConduccionPeticion;
import pe.edu.unc.elmirador.ejecucion.clients.dto.IncidenciaPeticion;
import pe.edu.unc.elmirador.ejecucion.exceptions.ConductoresIntegrationException;
import pe.edu.unc.elmirador.ejecucion.exceptions.ConflictoDeRecursoException;

@Component
public class ConductoresGateway {

    private final ConductoresClient cliente;

    public ConductoresGateway(ConductoresClient cliente) {
        this.cliente = cliente;
    }

    public void reportarHoras(String conductorId, HorasConduccionPeticion peticion) {
        String clave = claveHoras(peticion.viajeId(), conductorId);
        try {
            cliente.reportarHoras(conductorId, clave, peticion);
        } catch (RetryableException fallo) {
            throw new ConductoresIntegrationException(
                    "Conductores no respondio al reportar horas para el conductor " + conductorId, fallo);
        } catch (FeignException.Conflict rechazo) {
            // CON-02: las horas acumuladas superarian el maximo normado. Respondio, y dijo que no.
            throw new ConflictoDeRecursoException(
                    "Conductores rechazo las " + peticion.horas() + " horas del conductor " + conductorId
                            + ": superarian el maximo normado (CON-02)");
        } catch (FeignException fallo) {
            throw new ConductoresIntegrationException(
                    "Conductores respondio " + fallo.status() + " al reportar horas para el conductor " + conductorId, fallo);
        }
    }

    public void reportarIncidencia(String conductorId, String incidenciaId, IncidenciaPeticion peticion) {
        String clave = claveIncidencia(peticion.viajeId(), conductorId, incidenciaId);
        try {
            cliente.reportarIncidencia(conductorId, clave, peticion);
        } catch (RetryableException fallo) {
            throw new ConductoresIntegrationException(
                    "Conductores no respondio al reportar la incidencia para el conductor " + conductorId, fallo);
        } catch (FeignException fallo) {
            throw new ConductoresIntegrationException(
                    "Conductores respondio " + fallo.status() + " al reportar la incidencia para el conductor " + conductorId, fallo);
        }
    }

    String claveHoras(String viajeId, String conductorId) {
        return viajeId + ":" + conductorId + ":horas";
    }

    String claveIncidencia(String viajeId, String conductorId, String incidenciaId) {
        return viajeId + ":" + conductorId + ":incidencia:" + incidenciaId;
    }
}
