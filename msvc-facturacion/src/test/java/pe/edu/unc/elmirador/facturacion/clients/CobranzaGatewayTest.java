package pe.edu.unc.elmirador.facturacion.clients;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import feign.FeignException;
import feign.Request;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import pe.edu.unc.elmirador.facturacion.clients.dto.CuentaPorCobrarCreadaRemoto;
import pe.edu.unc.elmirador.facturacion.exceptions.CobranzaIntegrationException;
import pe.edu.unc.elmirador.facturacion.models.entity.Factura;
import pe.edu.unc.elmirador.facturacion.models.vo.Detraccion;
import pe.edu.unc.elmirador.facturacion.models.vo.Dinero;
import pe.edu.unc.elmirador.facturacion.models.vo.SnapshotComercial;
import pe.edu.unc.elmirador.facturacion.models.vo.NumeroDeComprobante;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;

class CobranzaGatewayTest {

    private CobranzaClient cliente;
    private CobranzaGateway pasarela;

    @BeforeEach
    void preparar() {
        cliente = mock(CobranzaClient.class);
        pasarela = new CobranzaGateway(cliente);
    }

    @Test
    @DisplayName("caso feliz: crea cuenta por cobrar para factura a credito")
    void exitoCredito() {
        Factura factura = facturaDeEjemplo("CREDITO");
        when(cliente.crearCuentaPorCobrar(eq("fac-1"), any()))
                .thenReturn(ResponseEntity.status(201).body(new CuentaPorCobrarCreadaRemoto("fac-1", "cta-1")));

        pasarela.crearCuentaPorCobrar(factura);

        verify(cliente).crearCuentaPorCobrar(eq("fac-1"), any());
    }

    @Test
    @DisplayName("caso feliz: no hace nada para factura al contado")
    void exitoContado() {
        Factura factura = facturaDeEjemplo("CONTADO");

        pasarela.crearCuentaPorCobrar(factura);

        verify(cliente, never()).crearCuentaPorCobrar(any(), any());
    }

    @Test
    @DisplayName("404 se traduce a CobranzaIntegrationException")
    void fallo404() {
        Factura factura = facturaDeEjemplo("CREDITO");
        Request request = Request.create(Request.HttpMethod.POST, "url", new HashMap<>(), null, null, null);
        when(cliente.crearCuentaPorCobrar(eq("fac-1"), any()))
                .thenThrow(new FeignException.NotFound("Not found", request, null, null));
        
        assertThatThrownBy(() -> pasarela.crearCuentaPorCobrar(factura))
                .isInstanceOf(CobranzaIntegrationException.class)
                .hasMessageContaining("404");
    }

    @Test
    @DisplayName("RetryableException se traduce a CobranzaIntegrationException")
    void falloRetryable() {
        Factura factura = facturaDeEjemplo("CREDITO");
        Request request = Request.create(Request.HttpMethod.POST, "url", new HashMap<>(), null, null, null);
        when(cliente.crearCuentaPorCobrar(eq("fac-1"), any()))
                .thenThrow(new RetryableException(500, "Timeout", Request.HttpMethod.POST, (Long) null, request));
        
        assertThatThrownBy(() -> pasarela.crearCuentaPorCobrar(factura))
                .isInstanceOf(CobranzaIntegrationException.class)
                .hasMessageContaining("no respondio");
    }

    private Factura facturaDeEjemplo(String condicion) {
        SnapshotComercial snap = new SnapshotComercial("ord-1", "cli-1", new Dinero(new BigDecimal("100"), "PEN"), "PEN", OffsetDateTime.now(), condicion, 30);
        Detraccion det = Detraccion.sinDetraccion("PEN");
        Factura f = Factura.abrir("fac-1", snap, det);
        f.agregarLinea(new pe.edu.unc.elmirador.facturacion.models.entity.LineaDeFactura("lin-1", "ord-1", pe.edu.unc.elmirador.facturacion.models.vo.ConceptoFacturable.FLETE, "Flete", new Dinero(new BigDecimal("100"), "PEN")));
        f.registrarConformidad(new pe.edu.unc.elmirador.facturacion.models.vo.Conformidad(true, java.util.Collections.emptyList(), OffsetDateTime.now()));
        f.emitir(new NumeroDeComprobante("F001", 1), OffsetDateTime.now());
        return f;
    }
}
