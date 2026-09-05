package pe.edu.unc.elmirador.ejecucion.clients;

import org.springframework.stereotype.Component;

import feign.FeignException;
import feign.RetryableException;
import java.util.List;

import pe.edu.unc.elmirador.ejecucion.clients.dto.HojaDeRutaRemota;
import pe.edu.unc.elmirador.ejecucion.exceptions.ProgramacionIntegrationException;

@Component
public class ProgramacionGateway {

    private final ProgramacionClient cliente;

    public ProgramacionGateway(ProgramacionClient cliente) {
        this.cliente = cliente;
    }

    public HojaDeRutaDeViaje obtenerHojaDeRuta(String viajeId) {
        HojaDeRutaRemota remoto;
        try {
            remoto = cliente.obtenerHojaDeRuta(viajeId);
        } catch (RetryableException fallo) {
            throw new ProgramacionIntegrationException(
                    "Programacion no respondio al consultar la hoja de ruta del viaje " + viajeId, fallo);
        } catch (FeignException fallo) {
            throw new ProgramacionIntegrationException(
                    "Programacion respondio " + fallo.status() + " al consultar la hoja de ruta del viaje " + viajeId + ": " + fallo.contentUTF8(), fallo);
        }
        
        if (remoto == null || remoto.viajeId() == null || remoto.estado() == null
                || remoto.paradas() == null || remoto.paradas().isEmpty()) {
            throw new ProgramacionIntegrationException(
                    "Programacion respondio una hoja de ruta incompleta para el viaje " + viajeId);
        }

        List<HojaDeRutaDeViaje.ParadaPlanificada> paradas = remoto.paradas().stream()
                .map(p -> {
                    if (p.ubicacion() == null || p.ubicacion().direccion() == null) {
                        throw new ProgramacionIntegrationException(
                                "Programacion respondio una parada sin direccion en el viaje " + viajeId);
                    }
                    return new HojaDeRutaDeViaje.ParadaPlanificada(
                            p.secuencia(), p.ordenDeServicioId(), p.ubicacion().direccion());
                })
                .toList();

        return new HojaDeRutaDeViaje(
                remoto.viajeId(), remoto.estado(), remoto.unidadId(), remoto.conductorIds(), paradas);
    }
}
