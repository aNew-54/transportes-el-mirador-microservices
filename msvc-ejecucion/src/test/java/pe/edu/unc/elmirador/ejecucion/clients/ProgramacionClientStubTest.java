package pe.edu.unc.elmirador.ejecucion.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

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

import pe.edu.unc.elmirador.ejecucion.clients.dto.HojaDeRutaRemota;
import pe.edu.unc.elmirador.ejecucion.exceptions.ProgramacionIntegrationException;

@SpringBootTest(classes = ProgramacionClientStubTest.Config.class)
class ProgramacionClientStubTest {

    private static final HttpServer SERVIDOR;
    private static volatile String cuerpo = "";
    private static volatile int estado = 200;
    private static volatile long demoraMs = 0;

    static {
        try {
            SERVIDOR = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo levantar el stub", e);
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
        System.setProperty("clients.programacion.url", "http://127.0.0.1:" + SERVIDOR.getAddress().getPort());
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
        System.clearProperty("clients.programacion.url");
        System.clearProperty("spring.cloud.openfeign.client.config.default.read-timeout");
    }

    @Autowired
    private ProgramacionClient cliente;

    @Autowired
    private ProgramacionGateway pasarela;

    private static final String EJEMPLO_DEL_CONTRATO = """
            {
              "viajeId": "VIA-2026-00045",
              "estado": "DESPACHADO",
              "unidadId": "UNI-004",
              "conductorIds": ["CON-011"],
              "observaciones": "Coordinar con almacén del cliente antes de las 07:00.",
              "paradas": [
                { "secuencia": 1, "tipo": "CARGA",    "ordenDeServicioId": "ORD-2026-000123",
                  "ubicacion": { "direccion": "Jr. Ayacucho 450", "distrito": "Cajamarca", "referencia": "Almacén 2", "contacto": "+51 976 000 111" },
                  "horaEstimada": "2026-09-10T06:30:00-05:00" },
                { "secuencia": 2, "tipo": "DESCARGA", "ordenDeServicioId": "ORD-2026-000123",
                  "ubicacion": { "direccion": "Av. España 1200", "distrito": "Trujillo", "referencia": "Puerta 3", "contacto": "+51 944 222 333" },
                  "horaEstimada": "2026-09-10T14:00:00-05:00" }
              ]
            }
            """;

    @Test
    void decodificaElEjemploDelContratoCampoACampo() {
        cuerpo = EJEMPLO_DEL_CONTRATO;

        HojaDeRutaRemota remoto = cliente.obtenerHojaDeRuta("VIA-2026-00045");

        assertThat(remoto.viajeId()).isEqualTo("VIA-2026-00045");
        assertThat(remoto.estado()).isEqualTo("DESPACHADO");
        assertThat(remoto.unidadId()).isEqualTo("UNI-004");
        assertThat(remoto.conductorIds()).containsExactly("CON-011");
        assertThat(remoto.observaciones()).isEqualTo("Coordinar con almacén del cliente antes de las 07:00.");
        assertThat(remoto.paradas()).hasSize(2);
        assertThat(remoto.paradas().get(0).secuencia()).isEqualTo(1);
        assertThat(remoto.paradas().get(0).tipo()).isEqualTo("CARGA");
        assertThat(remoto.paradas().get(0).ubicacion().distrito()).isEqualTo("Cajamarca");
        assertThat(remoto.paradas().get(0).horaEstimada()).isEqualTo(OffsetDateTime.parse("2026-09-10T06:30:00-05:00"));
    }

    @Test
    void siElProveedorNoContestaATiempoElGatewayLoTraduce() {
        cuerpo = EJEMPLO_DEL_CONTRATO;
        demoraMs = 3000;

        assertThatThrownBy(() -> pasarela.obtenerHojaDeRuta("VIA-2026-00045"))
                .isInstanceOf(ProgramacionIntegrationException.class);
    }

    @Test
    void unCuatrocientosCuatroDelProveedorLlegaTraducido() {
        estado = 404;

        assertThatThrownBy(() -> pasarela.obtenerHojaDeRuta("VIA-9999"))
                .isInstanceOf(ProgramacionIntegrationException.class)
                .hasMessageContaining("404");
    }

    @Configuration
    @EnableFeignClients(clients = ProgramacionClient.class)
    @ImportAutoConfiguration({
            FeignAutoConfiguration.class,
            HttpMessageConvertersAutoConfiguration.class,
            JacksonAutoConfiguration.class
    })
    static class Config {

        @org.springframework.context.annotation.Bean
        ProgramacionGateway programacionGateway(ProgramacionClient cliente) {
            return new ProgramacionGateway(cliente);
        }
    }
}
