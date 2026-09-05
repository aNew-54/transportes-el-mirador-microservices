package pe.edu.unc.elmirador.facturacion.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

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

import com.sun.net.httpserver.HttpServer;

import pe.edu.unc.elmirador.facturacion.clients.dto.SnapshotFacturableRemoto;
import pe.edu.unc.elmirador.facturacion.exceptions.ComercialIntegrationException;

@SpringBootTest(classes = ComercialClientStubTest.Config.class)
class ComercialClientStubTest {

    private static final HttpServer SERVIDOR;
    private static volatile String cuerpo = "";
    private static volatile int estado = 200;
    private static volatile long demoraMs = 0;

    static {
        try {
            SERVIDOR = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo levantar el stub de Comercial", e);
        }
        SERVIDOR.createContext("/", intercambio -> {
            if (demoraMs > 0) {
                try {
                    Thread.sleep(demoraMs);
                } catch (InterruptedException ignorada) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] datos = cuerpo.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(estado, datos.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(datos);
            }
        });
        SERVIDOR.setExecutor(Executors.newFixedThreadPool(4));
        SERVIDOR.start();
        System.setProperty("clients.comercial.url", "http://127.0.0.1:" + SERVIDOR.getAddress().getPort());
        System.setProperty("spring.cloud.openfeign.client.config.default.read-timeout", "800");
    }

    @BeforeEach
    void reiniciarElStub() {
        cuerpo = "{}";
        estado = 200;
        demoraMs = 0;
    }

    @AfterAll
    static void apagar() {
        SERVIDOR.stop(0);
        System.clearProperty("clients.comercial.url");
        System.clearProperty("spring.cloud.openfeign.client.config.default.read-timeout");
    }

    @Autowired
    private ComercialClient cliente;

    @Autowired
    private ComercialGateway pasarela;

    private static final String EJEMPLO_DEL_CONTRATO = """
            {
              "ordenId": "ORD-2026-000123",
              "clienteId": "CLI-0007",
              "ruc": "20481234567",
              "razonSocial": "Distribuidora Norte S.A.C.",
              "tarifa": {
                "fleteBase": { "monto": "1800.00", "moneda": "PEN" },
                "recargos":  [ { "tipo": "SOBRECAPACIDAD", "porcentaje": 10 } ],
                "descuento": { "porcentaje": 8, "motivo": "CONSOLIDACION" },
                "total":     { "monto": "1821.60", "moneda": "PEN" }
              },
              "condicionDePago": { "modalidad": "CREDITO", "plazoEnDias": 30 },
              "tomadoEn": "2026-09-10T16:00:00-05:00"
            }
            """;

    @Test
    void decodificaElEjemploDelContratoCampoACampo() {
        cuerpo = EJEMPLO_DEL_CONTRATO;

        SnapshotFacturableRemoto remoto = cliente.snapshotFacturableDe("ORD-2026-000123");

        assertThat(remoto.ordenId()).isEqualTo("ORD-2026-000123");
        assertThat(remoto.clienteId()).isEqualTo("CLI-0007");
        assertThat(remoto.tarifa().total().monto()).isEqualTo("1821.60");
        assertThat(remoto.tarifa().total().moneda()).isEqualTo("PEN");
        assertThat(remoto.condicionDePago().modalidad()).isEqualTo("CREDITO");
        assertThat(remoto.condicionDePago().plazoEnDias()).isEqualTo(30);
    }

    @Test
    void siElProveedorNoContestaATiempoElGatewayLoTraduce() {
        cuerpo = EJEMPLO_DEL_CONTRATO;
        demoraMs = 3000;

        assertThatThrownBy(() -> pasarela.snapshotFacturableDe("ORD-2026-000123"))
                .isInstanceOf(ComercialIntegrationException.class);
    }

    @Test
    void unCuatrocientosCuatroDelProveedorLlegaTraducido() {
        estado = 404;

        assertThatThrownBy(() -> pasarela.snapshotFacturableDe("ORD-9999"))
                .isInstanceOf(ComercialIntegrationException.class)
                .hasMessageContaining("404");
    }

    @Configuration
    @EnableFeignClients(clients = ComercialClient.class)
    @ImportAutoConfiguration({
            FeignAutoConfiguration.class,
            HttpMessageConvertersAutoConfiguration.class,
            JacksonAutoConfiguration.class
    })
    static class Config {
        @org.springframework.context.annotation.Bean
        ComercialGateway comercialGateway(ComercialClient cliente) {
            return new ComercialGateway(cliente);
        }
    }
}
