package pe.edu.unc.elmirador.programacion.clients;

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

import pe.edu.unc.elmirador.programacion.clients.dto.OrdenRemota;
import pe.edu.unc.elmirador.programacion.exceptions.ComercialIntegrationException;
import pe.edu.unc.elmirador.programacion.models.vo.TipoDeCarga;

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
              "estado": "CONFIRMADA",
              "carga": { "pesoKg": 8500, "volumenM3": 24.5, "tipo": "PALETIZADA", "embalaje": "PALLETS", "naturaleza": "ALIMENTARIA" },
              "ruta": { "origen": "Cajamarca", "destino": "Trujillo", "corredor": "COSTA_NORTE", "distanciaKm": 296 },
              "ventana": { "inicio": "2026-09-10T06:00:00-05:00", "fin": "2026-09-10T18:00:00-05:00" },
              "permiteConsolidacion": true,
              "restriccionesConsolidacion": ["SOLO_CARGA_ALIMENTARIA"],
              "tipoUnidadRequerido": "FURGON"
            }
            """;

    @Test
    void decodificaElEjemploDelContratoCampoACampo() {
        cuerpo = EJEMPLO_DEL_CONTRATO;

        OrdenRemota remoto = cliente.obtenerOrden("ORD-2026-000123");

        assertThat(remoto.ordenId()).isEqualTo("ORD-2026-000123");
        assertThat(remoto.clienteId()).isEqualTo("CLI-0007");
        assertThat(remoto.estado()).isEqualTo("CONFIRMADA");
        assertThat(remoto.carga().pesoKg()).isEqualTo(8500);
        assertThat(remoto.carga().tipo()).isEqualTo("PALETIZADA");
        assertThat(remoto.carga().embalaje()).isEqualTo("PALLETS");
        assertThat(remoto.ruta().origen()).isEqualTo("Cajamarca");
        assertThat(remoto.ventana().inicio()).isEqualTo(OffsetDateTime.parse("2026-09-10T06:00:00-05:00"));
        assertThat(remoto.permiteConsolidacion()).isTrue();
        assertThat(remoto.restriccionesConsolidacion()).containsExactly("SOLO_CARGA_ALIMENTARIA");
        assertThat(remoto.tipoUnidadRequerido()).isEqualTo("FURGON");
        
        OrdenConfirmada orden = pasarela.obtenerOrden("ORD-2026-000123");
        assertThat(orden.tipo()).isEqualTo(TipoDeCarga.PALETIZADA);
    }

    @Test
    void siElProveedorNoContestaATiempoElGatewayLoTraduce() {
        cuerpo = EJEMPLO_DEL_CONTRATO;
        demoraMs = 3000;

        assertThatThrownBy(() -> pasarela.obtenerOrden("ORD-2026-000123"))
                .isInstanceOf(ComercialIntegrationException.class);
    }

    @Test
    void unCuatrocientosCuatroDelProveedorLlegaTraducido() {
        estado = 404;

        assertThatThrownBy(() -> pasarela.obtenerOrden("ORD-9999"))
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
