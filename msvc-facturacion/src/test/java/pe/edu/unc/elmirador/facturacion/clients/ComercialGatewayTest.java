package pe.edu.unc.elmirador.facturacion.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import feign.FeignException;
import feign.Request;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pe.edu.unc.elmirador.facturacion.clients.dto.SnapshotFacturableRemoto;
import pe.edu.unc.elmirador.facturacion.clients.dto.SnapshotFacturableRemoto.*;
import pe.edu.unc.elmirador.facturacion.exceptions.ComercialIntegrationException;
import pe.edu.unc.elmirador.facturacion.models.vo.SnapshotComercial;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;

class ComercialGatewayTest {

    private ComercialClient cliente;
    private ComercialGateway pasarela;

    @BeforeEach
    void preparar() {
        cliente = mock(ComercialClient.class);
        pasarela = new ComercialGateway(cliente);
    }

    @Test
    @DisplayName("caso feliz: el snapshot se traduce correctamente")
    void exito() {
        SnapshotFacturableRemoto remoto = new SnapshotFacturableRemoto(
                "ord-1", "cli-1", "20481234567", "Razon",
                new TarifaRemota(null, Collections.emptyList(), null, new DineroRemoto("100.00", "PEN")),
                new CondicionDePagoRemota("CREDITO", 30),
                OffsetDateTime.now()
        );
        when(cliente.snapshotFacturableDe("ord-1")).thenReturn(remoto);

        SnapshotComercial snap = pasarela.snapshotFacturableDe("ord-1");
        assertThat(snap.tarifa().monto()).isEqualTo(new BigDecimal("100.00"));
        assertThat(snap.condicionDePagoModalidad()).isEqualTo("CREDITO");
        assertThat(snap.condicionDePagoPlazo()).isEqualTo(30);
    }

    @Test
    @DisplayName("404 se traduce a ComercialIntegrationException")
    void fallo404() {
        Request request = Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, null, null);
        when(cliente.snapshotFacturableDe("ord-1"))
                .thenThrow(new FeignException.NotFound("Not found", request, null, null));
        
        assertThatThrownBy(() -> pasarela.snapshotFacturableDe("ord-1"))
                .isInstanceOf(ComercialIntegrationException.class)
                .hasMessageContaining("404");
    }

    @Test
    @DisplayName("RetryableException se traduce a ComercialIntegrationException")
    void falloRetryable() {
        Request request = Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, null, null);
        when(cliente.snapshotFacturableDe("ord-1"))
                .thenThrow(new RetryableException(500, "Timeout", Request.HttpMethod.GET, (Long) null, request));
        
        assertThatThrownBy(() -> pasarela.snapshotFacturableDe("ord-1"))
                .isInstanceOf(ComercialIntegrationException.class)
                .hasMessageContaining("no respondio");
    }

    @Test
    @DisplayName("Cuerpo nulo o ilegible se traduce a ComercialIntegrationException")
    void cuerpoIlegible() {
        SnapshotFacturableRemoto remoto = new SnapshotFacturableRemoto(
                "ord-1", "cli-1", "20481234567", "Razon",
                null,
                null,
                OffsetDateTime.now()
        );
        when(cliente.snapshotFacturableDe("ord-1")).thenReturn(remoto);

        assertThatThrownBy(() -> pasarela.snapshotFacturableDe("ord-1"))
                .isInstanceOf(ComercialIntegrationException.class)
                .hasMessageContaining("incompleto");
    }
}
