package pe.edu.unc.elmirador.facturacion.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.ArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;

import com.sun.net.httpserver.HttpServer;

import pe.edu.unc.elmirador.facturacion.clients.dto.CuentaPorCobrarCreadaRemoto;
import pe.edu.unc.elmirador.facturacion.clients.dto.CuentaPorCobrarRequestRemoto;
import pe.edu.unc.elmirador.facturacion.exceptions.CobranzaIntegrationException;
import pe.edu.unc.elmirador.facturacion.models.entity.Factura;
import pe.edu.unc.elmirador.facturacion.models.vo.Detraccion;
import pe.edu.unc.elmirador.facturacion.models.vo.Dinero;
import pe.edu.unc.elmirador.facturacion.models.vo.SnapshotComercial;
import pe.edu.unc.elmirador.facturacion.models.vo.NumeroDeComprobante;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@SpringBootTest(classes = CobranzaClientStubTest.Config.class)
class CobranzaClientStubTest {

    private static final HttpServer SERVIDOR;
    private static volatile String cuerpo = "";
    private static volatile int estado = 201;
    private static volatile long demoraMs = 0;
    private static final List<String> idempotencyKeys = new ArrayList<>();

    static {
        try {
            SERVIDOR = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo levantar el stub de Cobranza", e);
        }
        SERVIDOR.createContext("/", intercambio -> {
            if (demoraMs > 0) {
                try {
                    Thread.sleep(demoraMs);
                } catch (InterruptedException ignorada) {
                    Thread.currentThread().interrupt();
                }
            }
            String ik = intercambio.getRequestHeaders().getFirst("Idempotency-Key");
            if (ik != null) idempotencyKeys.add(ik);
            
            byte[] datos = cuerpo.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(estado, datos.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(datos);
            }
        });
        SERVIDOR.setExecutor(Executors.newFixedThreadPool(4));
        SERVIDOR.start();
        System.setProperty("clients.cobranza.url", "http://127.0.0.1:" + SERVIDOR.getAddress().getPort());
        System.setProperty("spring.cloud.openfeign.client.config.default.read-timeout", "800");
    }

    @BeforeEach
    void reiniciarElStub() {
        cuerpo = "{}";
        estado = 201;
        demoraMs = 0;
        idempotencyKeys.clear();
    }

    @AfterAll
    static void apagar() {
        SERVIDOR.stop(0);
        System.clearProperty("clients.cobranza.url");
        System.clearProperty("spring.cloud.openfeign.client.config.default.read-timeout");
    }

    @Autowired
    private CobranzaClient cliente;

    @Autowired
    private CobranzaGateway pasarela;

    private static final String EJEMPLO_DEL_CONTRATO_RESPONSE = """
            {
              "facturaId": "FAC-2026-000310",
              "cuentaId": "CTA-2026-000310"
            }
            """;

    @Test
    void casoFeliz() {
        cuerpo = EJEMPLO_DEL_CONTRATO_RESPONSE;
        estado = 201;
        
        Factura f = facturaDeEjemplo();
        pasarela.crearCuentaPorCobrar(f);

        assertThat(idempotencyKeys).containsExactly(f.id());
    }

    @Test
    void siElProveedorNoContestaATiempoElGatewayLoTraduce() {
        cuerpo = EJEMPLO_DEL_CONTRATO_RESPONSE;
        demoraMs = 3000;
        
        Factura f = facturaDeEjemplo();
        assertThatThrownBy(() -> pasarela.crearCuentaPorCobrar(f))
                .isInstanceOf(CobranzaIntegrationException.class);
    }

    @Test
    void unCuatrocientosCuatroDelProveedorLlegaTraducido() {
        estado = 404;
        
        Factura f = facturaDeEjemplo();
        assertThatThrownBy(() -> pasarela.crearCuentaPorCobrar(f))
                .isInstanceOf(CobranzaIntegrationException.class)
                .hasMessageContaining("404");
    }

    @Test
    void dosPOSTConsecutivosMandanLaMismaIdempotencyKey() {
        cuerpo = EJEMPLO_DEL_CONTRATO_RESPONSE;
        estado = 201;
        
        Factura f = facturaDeEjemplo();
        pasarela.crearCuentaPorCobrar(f);
        
        estado = 200; // El reintento devuelve 200
        pasarela.crearCuentaPorCobrar(f);

        assertThat(idempotencyKeys).hasSize(2);
        assertThat(idempotencyKeys.get(0)).isEqualTo(f.id());
        assertThat(idempotencyKeys.get(1)).isEqualTo(f.id());
    }

    private Factura facturaDeEjemplo() {
        SnapshotComercial snap = new SnapshotComercial("ord-1", "cli-1", new Dinero(new BigDecimal("100"), "PEN"), "PEN", OffsetDateTime.now(), "CREDITO", 30);
        Detraccion det = Detraccion.sinDetraccion("PEN");
        Factura f = Factura.abrir("fac-1", snap, det);
        f.agregarLinea(new pe.edu.unc.elmirador.facturacion.models.entity.LineaDeFactura("lin-1", "ord-1", pe.edu.unc.elmirador.facturacion.models.vo.ConceptoFacturable.FLETE, "Flete", new Dinero(new BigDecimal("100"), "PEN")));
        f.registrarConformidad(new pe.edu.unc.elmirador.facturacion.models.vo.Conformidad(true, java.util.Collections.emptyList(), OffsetDateTime.now()));
        f.emitir(new NumeroDeComprobante("F001", 1), OffsetDateTime.now());
        return f;
    }

    @Configuration
    @EnableFeignClients(clients = CobranzaClient.class)
    @ImportAutoConfiguration({
            FeignAutoConfiguration.class,
            HttpMessageConvertersAutoConfiguration.class,
            JacksonAutoConfiguration.class
    })
    static class Config {
        @org.springframework.context.annotation.Bean
        CobranzaGateway cobranzaGateway(CobranzaClient cliente) {
            return new CobranzaGateway(cliente);
        }
    }
}
