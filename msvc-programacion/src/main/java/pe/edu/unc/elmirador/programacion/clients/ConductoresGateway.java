package pe.edu.unc.elmirador.programacion.clients;

import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;
import feign.FeignException;
import feign.RetryableException;
import pe.edu.unc.elmirador.programacion.clients.dto.ElegibilidadConductorRemota;
import pe.edu.unc.elmirador.programacion.exceptions.ConductoresIntegrationException;
import pe.edu.unc.elmirador.programacion.models.vo.ElegibilidadDeRecurso;

@Component
public class ConductoresGateway {
    private final ConductoresClient cliente;

    public ConductoresGateway(ConductoresClient cliente) {
        this.cliente = cliente;
    }

    public ElegibilidadDeRecurso consultarElegibilidad(
            String conductorId,
            OffsetDateTime desde,
            OffsetDateTime hasta,
            String tipoUnidad,
            String clienteId) {
        ElegibilidadConductorRemota remoto;
        try {
            remoto = cliente.consultarElegibilidad(conductorId, desde, hasta, tipoUnidad, clienteId);
        } catch (RetryableException fallo) {
            throw new ConductoresIntegrationException("Conductores no respondio al consultar el conductor " + conductorId, fallo);
        } catch (FeignException fallo) {
            throw new ConductoresIntegrationException("Conductores respondio " + fallo.status() + " al consultar el conductor " + conductorId, fallo);
        }
        
        if (remoto == null || remoto.motivos() == null) {
            throw new ConductoresIntegrationException("Conductores respondio una elegibilidad incompleta para el conductor " + conductorId);
        }
        
        return remoto.elegible() 
            ? ElegibilidadDeRecurso.recursoElegible() 
            : ElegibilidadDeRecurso.recursoNoElegible(remoto.motivos());
    }
}
