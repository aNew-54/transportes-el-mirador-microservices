package pe.edu.unc.elmirador.ejecucion.clients;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.RetryableException;
import pe.edu.unc.elmirador.ejecucion.clients.dto.ConformidadPeticion;
import pe.edu.unc.elmirador.ejecucion.exceptions.FacturacionIntegrationException;

class FacturacionGatewayTest {

    private FacturacionClient cliente;
    private FacturacionGateway pasarela;

    @BeforeEach
    void setUp() {
        cliente = mock(FacturacionClient.class);
        pasarela = new FacturacionGateway(cliente);
    }

    private static Request peticionFalsa() {
        return Request.create(Request.HttpMethod.POST, "/internal/v1/conformidades",
                java.util.Map.of(), null, new RequestTemplate());
    }

    @Test
    void registrarConformidadExito() {
        ConformidadPeticion peticion = new ConformidadPeticion("VIA-1", "ORD-1", "FIRMADA", OffsetDateTime.now(), List.of(), List.of());
        
        pasarela.registrarConformidad(peticion);
        
        verify(cliente).registrarConformidad(eq("VIA-1:ORD-1:conformidad"), eq(peticion));
    }

    @Test
    void fallaEnConformidadPorTimeoutEsFalloDeIntegracion() {
        ConformidadPeticion peticion = new ConformidadPeticion("VIA-1", "ORD-1", "FIRMADA", OffsetDateTime.now(), List.of(), List.of());
        
        org.mockito.Mockito.doThrow(new RetryableException(-1, "Connection refused", Request.HttpMethod.POST, (Long) null, peticionFalsa()))
            .when(cliente).registrarConformidad(any(), any());

        assertThatThrownBy(() -> pasarela.registrarConformidad(peticion))
                .isInstanceOf(FacturacionIntegrationException.class);
    }

    @Test
    void fallaEnConformidadPor400EsFalloDeIntegracion() {
        ConformidadPeticion peticion = new ConformidadPeticion("VIA-1", "ORD-1", "FIRMADA", OffsetDateTime.now(), List.of(), List.of());
        
        org.mockito.Mockito.doThrow(new FeignException.BadRequest("bad request", peticionFalsa(), null, null))
            .when(cliente).registrarConformidad(any(), any());

        assertThatThrownBy(() -> pasarela.registrarConformidad(peticion))
                .isInstanceOf(FacturacionIntegrationException.class);
    }
}
