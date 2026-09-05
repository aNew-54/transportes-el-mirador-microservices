package pe.edu.unc.elmirador.ejecucion.clients;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.RetryableException;
import pe.edu.unc.elmirador.ejecucion.clients.dto.HorasConduccionPeticion;
import pe.edu.unc.elmirador.ejecucion.clients.dto.IncidenciaPeticion;
import pe.edu.unc.elmirador.ejecucion.exceptions.ConductoresIntegrationException;

class ConductoresGatewayTest {

    private ConductoresClient cliente;
    private ConductoresGateway pasarela;

    @BeforeEach
    void setUp() {
        cliente = mock(ConductoresClient.class);
        pasarela = new ConductoresGateway(cliente);
    }

    private static Request peticionFalsa() {
        return Request.create(Request.HttpMethod.POST, "/internal/v1/conductores/CON-1/horas-conduccion",
                java.util.Map.of(), null, new RequestTemplate());
    }

    @Test
    void reportarHorasExito() {
        HorasConduccionPeticion peticion = new HorasConduccionPeticion("VIA-1", 4.5, OffsetDateTime.now(), OffsetDateTime.now());
        
        pasarela.reportarHoras("CON-1", peticion);
        
        verify(cliente).reportarHoras(eq("CON-1"), eq("VIA-1:CON-1:horas"), eq(peticion));
    }

    @Test
    void fallaEnHorasPorTimeoutEsFalloDeIntegracion() {
        HorasConduccionPeticion peticion = new HorasConduccionPeticion("VIA-1", 4.5, OffsetDateTime.now(), OffsetDateTime.now());
        
        org.mockito.Mockito.doThrow(new RetryableException(-1, "Connection refused", Request.HttpMethod.POST, (Long) null, peticionFalsa()))
            .when(cliente).reportarHoras(any(), any(), any());

        assertThatThrownBy(() -> pasarela.reportarHoras("CON-1", peticion))
                .isInstanceOf(ConductoresIntegrationException.class);
    }

    @Test
    void fallaEnHorasPor409EsFalloDeIntegracion() {
        HorasConduccionPeticion peticion = new HorasConduccionPeticion("VIA-1", 4.5, OffsetDateTime.now(), OffsetDateTime.now());
        
        org.mockito.Mockito.doThrow(new FeignException.Conflict("conflict", peticionFalsa(), null, null))
            .when(cliente).reportarHoras(any(), any(), any());

        assertThatThrownBy(() -> pasarela.reportarHoras("CON-1", peticion))
                .isInstanceOf(ConductoresIntegrationException.class);
    }
    
    @Test
    void reportarIncidenciaExito() {
        IncidenciaPeticion peticion = new IncidenciaPeticion("VIA-1", "DOC", "Desc", true);
        
        pasarela.reportarIncidencia("CON-1", "INC-1", peticion);
        
        verify(cliente).reportarIncidencia(eq("CON-1"), eq("VIA-1:CON-1:incidencia:INC-1"), eq(peticion));
    }
}
