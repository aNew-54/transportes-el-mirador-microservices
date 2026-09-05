package pe.edu.unc.elmirador.programacion.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.math.BigDecimal;

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

import pe.edu.unc.elmirador.programacion.clients.dto.ElegibilidadUnidadRemota;
import pe.edu.unc.elmirador.programacion.exceptions.UnidadesIntegrationException;

@SpringBootTest(classes = UnidadesClientStubTest.Config.class)
class UnidadesClientStubTest {

    private static final HttpServer SERVIDOR;
    private static volatile String cuerpo = "";
    private static volatile int estado = 200;
    private static volatile long demoraMs = 0;

    static {
        try {
            SERVIDOR = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo levantar el stub de Unidades", e);
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
        System.setProperty("clients.unidades.url", "http://127.0.0.1:" + SERVIDOR.getAddress().getPort());
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
        System.clearProperty("clients.unidades.url");
        System.clearProperty("spring.cloud.openfeign.client.config.default.read-timeout");
    }

    @Autowired
    private UnidadesClient cliente;

    @Autowired
    private UnidadesGateway pasarela;

    private static final String EJEMPLO_DEL_CONTRATO = """
            {
              "unidadId": "UNI-004",
              "elegible": false,
              "motivos": ["DOCUMENTO_VENCIDO:SOAT", "MANTENIMIENTO_VENCIDO"],
              "capacidad": { "pesoMaximoKg": 10000, "volumenMaximoM3": 32.0 },
              "tipoUnidad": "FURGON",
              "estadoOperativo": "INOPERATIVA"
            }
            """;
            
    private final OffsetDateTime desde = OffsetDateTime.parse("2026-09-10T06:00:00-05:00");
    private final OffsetDateTime hasta = OffsetDateTime.parse("2026-09-10T18:00:00-05:00");

    @Test
    void decodificaElEjemploDelContratoCampoACampo() {
        cuerpo = EJEMPLO_DEL_CONTRATO;

        ElegibilidadUnidadRemota remoto = cliente.consultarElegibilidad("UNI-004", desde, hasta, 1000, new BigDecimal("2.5"), "GENERAL");

        assertThat(remoto.unidadId()).isEqualTo("UNI-004");
        assertThat(remoto.elegible()).isFalse();
        assertThat(remoto.motivos()).containsExactly("DOCUMENTO_VENCIDO:SOAT", "MANTENIMIENTO_VENCIDO");
        assertThat(remoto.capacidad().pesoMaximoKg()).isEqualTo(10000);
        assertThat(remoto.capacidad().volumenMaximoM3()).isEqualTo(new BigDecimal("32.0"));
        assertThat(remoto.tipoUnidad()).isEqualTo("FURGON");
        assertThat(remoto.estadoOperativo()).isEqualTo("INOPERATIVA");
        
        EvaluacionDeUnidad eval = pasarela.consultarElegibilidad("UNI-004", desde, hasta, 1000, new BigDecimal("2.5"), "GENERAL");
        assertThat(eval.elegibilidad().elegible()).isFalse();
    }

    @Test
    void siElProveedorNoContestaATiempoElGatewayLoTraduce() {
        cuerpo = EJEMPLO_DEL_CONTRATO;
        demoraMs = 3000;

        assertThatThrownBy(() -> pasarela.consultarElegibilidad("UNI-004", desde, hasta, 1000, new BigDecimal("2.5"), "GENERAL"))
                .isInstanceOf(UnidadesIntegrationException.class);
    }

    @Test
    void unCuatrocientosCuatroDelProveedorLlegaTraducido() {
        estado = 404;

        assertThatThrownBy(() -> pasarela.consultarElegibilidad("UNI-9999", desde, hasta, 1000, new BigDecimal("2.5"), "GENERAL"))
                .isInstanceOf(UnidadesIntegrationException.class)
                .hasMessageContaining("404");
    }

    @Configuration
    @EnableFeignClients(clients = UnidadesClient.class)
    @ImportAutoConfiguration({
            FeignAutoConfiguration.class,
            HttpMessageConvertersAutoConfiguration.class,
            JacksonAutoConfiguration.class
    })
    static class Config {
        @org.springframework.context.annotation.Bean
        UnidadesGateway unidadesGateway(UnidadesClient cliente) {
            return new UnidadesGateway(cliente);
        }
    }
}
