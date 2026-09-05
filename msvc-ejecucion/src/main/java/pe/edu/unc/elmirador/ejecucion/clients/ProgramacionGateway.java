package pe.edu.unc.elmirador.ejecucion.clients;

import org.springframework.stereotype.Component;

import feign.FeignException;
import feign.RetryableException;
import pe.edu.unc.elmirador.ejecucion.clients.dto.HojaDeRutaRemota;
import pe.edu.unc.elmirador.ejecucion.exceptions.ProgramacionIntegrationException;

@Component
public class ProgramacionGateway {

    private final ProgramacionClient cliente;

    public ProgramacionGateway(ProgramacionClient cliente) {
        this.cliente = cliente;
    }

    public HojaDeRutaRemota obtenerHojaDeRuta(String viajeId) {
        HojaDeRutaRemota remoto;
        try {
            remoto = cliente.obtenerHojaDeRuta(viajeId);
        } catch (RetryableException fallo) {
            throw new ProgramacionIntegrationException(
                    "Programacion no respondio al consultar la hoja de ruta del viaje " + viajeId, fallo);
        } catch (FeignException fallo) {
            throw new ProgramacionIntegrationException(
                    "Programacion respondio " + fallo.status() + " al consultar la hoja de ruta del viaje " + viajeId, fallo);
        }
        
        if (remoto == null || remoto.viajeId() == null || remoto.estado() == null) {
            throw new ProgramacionIntegrationException("Programacion respondio una hoja de ruta incompleta para el viaje " + viajeId);
        }
        
        return remoto;
    }
}
