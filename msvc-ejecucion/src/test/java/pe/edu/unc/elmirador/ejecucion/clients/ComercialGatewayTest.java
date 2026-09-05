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
import pe.edu.unc.elmirador.ejecucion.clients.dto.CargaRemota;
import pe.edu.unc.elmirador.ejecucion.clients.dto.DiferenciaCargaPeticion;
import pe.edu.unc.elmirador.ejecucion.clients.dto.ImporteRemoto;
import pe.edu.unc.elmirador.ejecucion.exceptions.ComercialIntegrationException;

class ComercialGatewayTest {

    private ComercialClient cliente;
    private ComercialGateway pasarela;

    @BeforeEach
    void setUp() {
        cliente = mock(ComercialClient.class);
        pasarela = new ComercialGateway(cliente);
    }

    private static Request peticionFalsa() {
        return Request.create(Request.HttpMethod.POST, "/internal/v1/ordenes/ORD-1/diferencias-de-carga",
                java.util.Map.of(), null, new RequestTemplate());
    }

    @Test
    void reportarDiferenciaExito() {
        DiferenciaCargaPeticion peticion = new DiferenciaCargaPeticion("VIA-1", 
                new CargaRemota(100, 1.0, "CAJAS"), 
                new CargaRemota(150, 1.5, "CAJAS"), 
                "ACEPTADA", 
                new ImporteRemoto("10.00", "PEN"), 
                OffsetDateTime.now());
        
        pasarela.reportarDiferencia("ORD-1", peticion);
        
        verify(cliente).reportarDiferencia(eq("ORD-1"), eq("VIA-1:ORD-1:diferencia"), eq(peticion));
    }

    @Test
    void fallaEnDiferenciaPorTimeoutEsFalloDeIntegracion() {
        DiferenciaCargaPeticion peticion = new DiferenciaCargaPeticion("VIA-1", 
                new CargaRemota(100, 1.0, "CAJAS"), 
                new CargaRemota(150, 1.5, "CAJAS"), 
                "ACEPTADA", 
                new ImporteRemoto("10.00", "PEN"), 
                OffsetDateTime.now());
        
        org.mockito.Mockito.doThrow(new RetryableException(-1, "Connection refused", Request.HttpMethod.POST, (Long) null, peticionFalsa()))
            .when(cliente).reportarDiferencia(any(), any(), any());

        assertThatThrownBy(() -> pasarela.reportarDiferencia("ORD-1", peticion))
                .isInstanceOf(ComercialIntegrationException.class);
    }

    @Test
    void fallaEnDiferenciaPor409EsFalloDeIntegracion() {
        DiferenciaCargaPeticion peticion = new DiferenciaCargaPeticion("VIA-1", 
                new CargaRemota(100, 1.0, "CAJAS"), 
                new CargaRemota(150, 1.5, "CAJAS"), 
                "ACEPTADA", 
                new ImporteRemoto("10.00", "PEN"), 
                OffsetDateTime.now());
        
        org.mockito.Mockito.doThrow(new FeignException.Conflict("conflict", peticionFalsa(), null, null))
            .when(cliente).reportarDiferencia(any(), any(), any());

        assertThatThrownBy(() -> pasarela.reportarDiferencia("ORD-1", peticion))
                .isInstanceOf(ComercialIntegrationException.class);
    }
}
