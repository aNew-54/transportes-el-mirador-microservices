package pe.edu.unc.elmirador.programacion.clients;

import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
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
            remoto = cliente.consultarElegibilidad(conductorId, iso(desde), iso(hasta), tipoUnidad, clienteId);
        } catch (RetryableException fallo) {
            throw new ConductoresIntegrationException("Conductores no respondio al consultar el conductor " + conductorId, fallo);
        } catch (FeignException fallo) {
            throw new ConductoresIntegrationException("Conductores respondio " + fallo.status() + " al consultar el conductor " + conductorId + ": " + fallo.contentUTF8(), fallo);
        }
        
        if (remoto == null || remoto.motivos() == null) {
            throw new ConductoresIntegrationException("Conductores respondio una elegibilidad incompleta para el conductor " + conductorId);
        }
        
        return remoto.elegible() 
            ? ElegibilidadDeRecurso.recursoElegible() 
            : ElegibilidadDeRecurso.recursoNoElegible(remoto.motivos());
    }

    /**
     * La fecha tal como la regla 6 la exige: ISO 8601 con offset.
     *
     * <p>Feign expandiria el {@code OffsetDateTime} con el formateador del locale por defecto del
     * JVM —{@code 10/10/26, 1:00 p. m.}—, que ni es ISO ni conserva el offset, y el proveedor
     * responderia 400. La conversion vive aqui porque traducir al idioma del contrato es lo que
     * hace una pasarela.
     */
    static String iso(OffsetDateTime momento) {
        if (momento == null) {
            throw new IllegalArgumentException("La fecha del intervalo es obligatoria");
        }
        return momento.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

}
