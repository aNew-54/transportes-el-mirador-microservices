package pe.edu.unc.elmirador.programacion.clients;

import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
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
            remoto = cliente.consultarElegibilidad(unidadId, iso(desde), iso(hasta), pesoKg, volumenM3, tipoCargaRequerido);
        } catch (RetryableException fallo) {
            throw new UnidadesIntegrationException("Unidades no respondio al consultar la unidad " + unidadId, fallo);
        } catch (FeignException fallo) {
            throw new UnidadesIntegrationException("Unidades respondio " + fallo.status() + " al consultar la unidad " + unidadId + ": " + fallo.contentUTF8(), fallo);
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
