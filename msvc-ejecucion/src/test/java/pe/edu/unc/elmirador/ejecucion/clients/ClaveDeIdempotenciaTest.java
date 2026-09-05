package pe.edu.unc.elmirador.ejecucion.clients;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import pe.edu.unc.elmirador.ejecucion.clients.dto.KilometrajePeticion;

class ClaveDeIdempotenciaTest {

    @Test
    void generaMismaClaveParaMismosDatos() {
        UnidadesClient cliente = mock(UnidadesClient.class);
        UnidadesGateway pasarela = new UnidadesGateway(cliente);
        
        KilometrajePeticion peticion = new KilometrajePeticion("VIA-2026", 1500, OffsetDateTime.now());
        
        pasarela.reportarKilometraje("UNI-1", peticion);
        pasarela.reportarKilometraje("UNI-1", peticion);
        
        // Verifica que se llamó dos veces EXACTAMENTE con la misma clave generada a partir de la peticion
        verify(cliente, times(2)).reportarKilometraje(eq("UNI-1"), eq("VIA-2026:km-final"), eq(peticion));
    }
}
