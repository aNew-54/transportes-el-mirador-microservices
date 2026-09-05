package pe.edu.unc.elmirador.ejecucion.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.RetryableException;
import pe.edu.unc.elmirador.ejecucion.clients.dto.FallaPeticion;
import pe.edu.unc.elmirador.ejecucion.clients.dto.KilometrajePeticion;
import pe.edu.unc.elmirador.ejecucion.exceptions.UnidadesIntegrationException;

class UnidadesGatewayTest {

    private UnidadesClient cliente;
    private UnidadesGateway pasarela;

    @BeforeEach
    void setUp() {
        cliente = mock(UnidadesClient.class);
        pasarela = new UnidadesGateway(cliente);
    }

    private static Request peticionFalsa() {
        return Request.create(Request.HttpMethod.POST, "/internal/v1/unidades/UNI-1/kilometraje",
                java.util.Map.of(), null, new RequestTemplate());
    }

    @Test
    void reportarKilometrajeExito() {
        KilometrajePeticion peticion = new KilometrajePeticion("VIA-1", 1000, OffsetDateTime.now());
        
        pasarela.reportarKilometraje("UNI-1", peticion);
        
        verify(cliente).reportarKilometraje(eq("UNI-1"), eq("VIA-1:km-final"), eq(peticion));
    }

    @Test
    void fallaEnKilometrajePorTimeoutEsFalloDeIntegracion() {
        KilometrajePeticion peticion = new KilometrajePeticion("VIA-1", 1000, OffsetDateTime.now());
        
        // Use doThrow for void methods
        org.mockito.Mockito.doThrow(new RetryableException(-1, "Connection refused", Request.HttpMethod.POST, (Long) null, peticionFalsa()))
            .when(cliente).reportarKilometraje(any(), any(), any());

        assertThatThrownBy(() -> pasarela.reportarKilometraje("UNI-1", peticion))
                .isInstanceOf(UnidadesIntegrationException.class)
                .hasMessageContaining("no respondio");
    }

    @Test
    void fallaEnKilometrajePor409EsFalloDeIntegracion() {
        KilometrajePeticion peticion = new KilometrajePeticion("VIA-1", 1000, OffsetDateTime.now());
        
        org.mockito.Mockito.doThrow(new FeignException.Conflict("conflict", peticionFalsa(), null, null))
            .when(cliente).reportarKilometraje(any(), any(), any());

        assertThatThrownBy(() -> pasarela.reportarKilometraje("UNI-1", peticion))
                .isInstanceOf(UnidadesIntegrationException.class);
    }
    
    @Test
    void reportarFallaExito() {
        FallaPeticion peticion = new FallaPeticion("VIA-1", "MECANICA", "Desc", OffsetDateTime.now(), true);
        
        pasarela.reportarFalla("UNI-1", "FALLA-1", peticion);
        
        verify(cliente).reportarFalla(eq("UNI-1"), eq("VIA-1:falla:FALLA-1"), eq(peticion));
    }
}
