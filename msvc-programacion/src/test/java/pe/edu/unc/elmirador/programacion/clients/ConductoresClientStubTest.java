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

import pe.edu.unc.elmirador.programacion.clients.dto.ElegibilidadConductorRemota;
import pe.edu.unc.elmirador.programacion.exceptions.ConductoresIntegrationException;
import pe.edu.unc.elmirador.programacion.models.vo.ElegibilidadDeRecurso;

@SpringBootTest(classes = ConductoresClientStubTest.Config.class)
class ConductoresClientStubTest {

    private static final HttpServer SERVIDOR;
    private static volatile String cuerpo = "";
    private static volatile int estado = 200;
    private static volatile long demoraMs = 0;

    static {
        try {
            SERVIDOR = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo levantar el stub de Conductores", e);
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
        System.setProperty("clients.conductores.url", "http://127.0.0.1:" + SERVIDOR.getAddress().getPort());
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
        System.clearProperty("clients.conductores.url");
        System.clearProperty("spring.cloud.openfeign.client.config.default.read-timeout");
    }

    @Autowired
    private ConductoresClient cliente;

    @Autowired
    private ConductoresGateway pasarela;

    private static final String EJEMPLO_DEL_CONTRATO = """
            {
              "conductorId": "CON-011",
              "elegible": false,
              "motivos": ["INDUCCION_VENCIDA:CLI-0019", "HORAS_INSUFICIENTES"],
              "categoriaLicencia": "A-IIIB",
              "horasDisponibles": 3.5
            }
            """;
            
    private final OffsetDateTime desde = OffsetDateTime.parse("2026-09-10T06:00:00-05:00");
    private final OffsetDateTime hasta = OffsetDateTime.parse("2026-09-10T18:00:00-05:00");

    @Test
    void decodificaElEjemploDelContratoCampoACampo() {
        cuerpo = EJEMPLO_DEL_CONTRATO;

        ElegibilidadConductorRemota remoto = cliente.consultarElegibilidad("CON-011", desde, hasta, "FURGON", "CLI-0019");

        assertThat(remoto.conductorId()).isEqualTo("CON-011");
        assertThat(remoto.elegible()).isFalse();
        assertThat(remoto.motivos()).containsExactly("INDUCCION_VENCIDA:CLI-0019", "HORAS_INSUFICIENTES");
        assertThat(remoto.categoriaLicencia()).isEqualTo("A-IIIB");
        assertThat(remoto.horasDisponibles()).isEqualTo(new BigDecimal("3.5"));
        
        ElegibilidadDeRecurso eval = pasarela.consultarElegibilidad("CON-011", desde, hasta, "FURGON", "CLI-0019");
        assertThat(eval.elegible()).isFalse();
    }

    @Test
    void siElProveedorNoContestaATiempoElGatewayLoTraduce() {
        cuerpo = EJEMPLO_DEL_CONTRATO;
        demoraMs = 3000;

        assertThatThrownBy(() -> pasarela.consultarElegibilidad("CON-011", desde, hasta, "FURGON", "CLI-0019"))
                .isInstanceOf(ConductoresIntegrationException.class);
    }

    @Test
    void unCuatrocientosCuatroDelProveedorLlegaTraducido() {
        estado = 404;

        assertThatThrownBy(() -> pasarela.consultarElegibilidad("CON-9999", desde, hasta, "FURGON", "CLI-0019"))
                .isInstanceOf(ConductoresIntegrationException.class)
                .hasMessageContaining("404");
    }

    @Configuration
    @EnableFeignClients(clients = ConductoresClient.class)
    @ImportAutoConfiguration({
            FeignAutoConfiguration.class,
            HttpMessageConvertersAutoConfiguration.class,
            JacksonAutoConfiguration.class
    })
    static class Config {
        @org.springframework.context.annotation.Bean
        ConductoresGateway conductoresGateway(ConductoresClient cliente) {
            return new ConductoresGateway(cliente);
        }
    }
}
