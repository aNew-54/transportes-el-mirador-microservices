package pe.edu.unc.elmirador.comercial.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

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

import pe.edu.unc.elmirador.comercial.clients.dto.EstadoCrediticioRemoto;
import pe.edu.unc.elmirador.comercial.exceptions.CobranzaIntegrationException;

/**
 * El cliente Feign de verdad contra un servidor de verdad, sirviendo el JSON copiado de
 * {@code docs/api/contracts.md}.
 *
 * <p>Es lo unico que demuestra que {@link EstadoCrediticioRemoto} casa con la forma pactada. Un record
 * con un campo mal escrito compila, pasa {@link CobranzaGatewayTest} —que lo construye a mano— y falla
 * en produccion decodificando un {@code null}.
 *
 * <p>El servidor es el {@code HttpServer} del JDK y no WireMock: son veinte lineas, no arrastran una
 * dependencia nueva (regla 8) y hablan por un socket real, que es lo que hace falta para probar tambien
 * el vencimiento del {@code read-timeout}.
 */
@SpringBootTest(classes = CobranzaClientStubTest.Config.class)
class CobranzaClientStubTest {

    private static final HttpServer SERVIDOR;
    private static volatile String cuerpo = "";
    private static volatile int estado = 200;
    private static volatile long demoraMs = 0;

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
            byte[] datos = cuerpo.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(estado, datos.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(datos);
            }
        });
        // Con el ejecutor por defecto el servidor atiende en serie: el manejador que duerme para
        // provocar el timeout sigue durmiendo despues de que el cliente se rinda, y bloquea a las
        // pruebas siguientes, que fallan por un timeout que no es el suyo.
        SERVIDOR.setExecutor(Executors.newFixedThreadPool(4));
        SERVIDOR.start();
        System.setProperty("clients.cobranza.url", "http://127.0.0.1:" + SERVIDOR.getAddress().getPort());
        // Un read-timeout corto para que la prueba del proveedor mudo no tarde cinco segundos.
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
        System.clearProperty("clients.cobranza.url");
        System.clearProperty("spring.cloud.openfeign.client.config.default.read-timeout");
    }

    @Autowired
    private CobranzaClient cliente;

    @Autowired
    private CobranzaGateway pasarela;

    /** El ejemplo del contrato 11, copiado sin tocar. */
    private static final String EJEMPLO_DEL_CONTRATO = """
            {
              "clienteId": "CLI-0007",
              "situacion": "SUSPENDIDO",
              "fechaDeCambio": "2026-08-28",
              "diasDeAtrasoMaximo": 43,
              "cuentasVencidas": 2,
              "deudaPorMoneda": [ { "monto": "5420.30", "moneda": "PEN" }, { "monto": "800.00", "moneda": "USD" } ]
            }
            """;

    @Test
    void decodificaElEjemploDelContratoCampoACampo() {
        cuerpo = EJEMPLO_DEL_CONTRATO;

        EstadoCrediticioRemoto remoto = cliente.estadoCrediticio("CLI-0007");

        assertThat(remoto.clienteId()).isEqualTo("CLI-0007");
        assertThat(remoto.situacion()).isEqualTo("SUSPENDIDO");
        assertThat(remoto.fechaDeCambio()).isEqualTo(LocalDate.parse("2026-08-28"));
        assertThat(remoto.diasDeAtrasoMaximo()).isEqualTo(43);
        assertThat(remoto.cuentasVencidas()).isEqualTo(2);
        assertThat(remoto.deudaPorMoneda()).hasSize(2);
        // El monto viaja como texto justamente para que el 5420.30 no llegue como 5420.3.
        assertThat(remoto.deudaPorMoneda().get(0).monto()).isEqualTo("5420.30");
        assertThat(remoto.deudaPorMoneda().get(0).moneda()).isEqualTo("PEN");
        assertThat(remoto.deudaPorMoneda().get(1).monto()).isEqualTo("800.00");
    }

    @Test
    void unClienteSinDeudaTraeLaListaVacia() {
        cuerpo = """
                {
                  "clienteId": "CLI-0001",
                  "situacion": "VIGENTE",
                  "fechaDeCambio": "2026-01-15",
                  "diasDeAtrasoMaximo": 0,
                  "cuentasVencidas": 0,
                  "deudaPorMoneda": []
                }
                """;

        assertThat(pasarela.estadoCrediticioDe("CLI-0001").permiteCredito()).isTrue();
    }

    /** El proveedor tarda mas que el read-timeout. Es el caso de «Cobranza no responde». */
    @Test
    void siElProveedorNoContestaATiempoElGatewayLoTraduce() {
        cuerpo = EJEMPLO_DEL_CONTRATO;
        demoraMs = 3000;

        assertThatThrownBy(() -> pasarela.estadoCrediticioDe("CLI-0007"))
                .isInstanceOf(CobranzaIntegrationException.class);
    }

    @Test
    void unCuatrocientosCuatroDelProveedorLlegaTraducido() {
        estado = 404;

        assertThatThrownBy(() -> pasarela.estadoCrediticioDe("CLI-9999"))
                .isInstanceOf(CobranzaIntegrationException.class)
                .hasMessageContaining("404");
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
