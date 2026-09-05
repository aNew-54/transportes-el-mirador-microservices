package pe.edu.unc.elmirador.programacion.clients;

import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import feign.FeignException;
import feign.RetryableException;
import pe.edu.unc.elmirador.programacion.clients.dto.ElegibilidadUnidadRemota;
import pe.edu.unc.elmirador.programacion.exceptions.UnidadesIntegrationException;
import pe.edu.unc.elmirador.programacion.models.vo.ElegibilidadDeRecurso;
import pe.edu.unc.elmirador.programacion.models.vo.Capacidad;

@Component
public class UnidadesGateway {
    private final UnidadesClient cliente;

    public UnidadesGateway(UnidadesClient cliente) {
        this.cliente = cliente;
    }

    public EvaluacionDeUnidad consultarElegibilidad(
            String unidadId,
            OffsetDateTime desde,
            OffsetDateTime hasta,
            int pesoKg,
            BigDecimal volumenM3,
            String tipoCargaRequerido) {
        ElegibilidadUnidadRemota remoto;
        try {
            remoto = cliente.consultarElegibilidad(unidadId, desde, hasta, pesoKg, volumenM3, tipoCargaRequerido);
        } catch (RetryableException fallo) {
            throw new UnidadesIntegrationException("Unidades no respondio al consultar la unidad " + unidadId, fallo);
        } catch (FeignException fallo) {
            throw new UnidadesIntegrationException("Unidades respondio " + fallo.status() + " al consultar la unidad " + unidadId, fallo);
        }
        
        if (remoto == null || remoto.motivos() == null || remoto.capacidad() == null) {
            throw new UnidadesIntegrationException("Unidades respondio una elegibilidad incompleta para la unidad " + unidadId);
        }
        
        ElegibilidadDeRecurso elegibilidad = remoto.elegible() 
            ? ElegibilidadDeRecurso.recursoElegible() 
            : ElegibilidadDeRecurso.recursoNoElegible(remoto.motivos());
            
        Capacidad capacidad = new Capacidad(remoto.capacidad().pesoMaximoKg(), remoto.capacidad().volumenMaximoM3());
        
        return new EvaluacionDeUnidad(elegibilidad, capacidad, remoto.tipoUnidad());
    }
}
