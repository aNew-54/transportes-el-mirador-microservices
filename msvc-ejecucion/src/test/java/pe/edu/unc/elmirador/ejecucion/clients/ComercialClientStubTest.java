package pe.edu.unc.elmirador.ejecucion.clients;

import static org.assertj.core.api.Assertions.assertThatCode;
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

import pe.edu.unc.elmirador.ejecucion.clients.dto.CargaRemota;
import pe.edu.unc.elmirador.ejecucion.clients.dto.DiferenciaCargaPeticion;
import pe.edu.unc.elmirador.ejecucion.clients.dto.ImporteRemoto;
import pe.edu.unc.elmirador.ejecucion.exceptions.ComercialIntegrationException;

@SpringBootTest(classes = ComercialClientStubTest.Config.class)
class ComercialClientStubTest {

    private static final HttpServer SERVIDOR;
    private static volatile String cuerpo = "";
    private static volatile int estado = 200;
    private static volatile long demoraMs = 0;
    private static volatile String expectedIdempotencyKey = null;

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
            
            int finalEstado = estado;
            String idempotencyKey = intercambio.getRequestHeaders().getFirst("Idempotency-Key");
            if (expectedIdempotencyKey != null && !expectedIdempotencyKey.equals(idempotencyKey)) {
                finalEstado = 400; // Si no llego la cabecera con el valor esperado, 400
            }

            byte[] datos = cuerpo.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(finalEstado, datos.length);
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
        expectedIdempotencyKey = null;
    }

    @AfterAll
    static void apagar() {
        SERVIDOR.stop(0);
        System.clearProperty("clients.comercial.url");
        System.clearProperty("spring.cloud.openfeign.client.config.default.read-timeout");
    }

    @Autowired
    private ComercialGateway pasarela;

    @Test
    void enviaCabeceraIdempotenciaCorrectamenteYRespondeBien() {
        cuerpo = "{}"; 
        expectedIdempotencyKey = "VIA-1:ORD-1:diferencia";
        
        DiferenciaCargaPeticion peticion = new DiferenciaCargaPeticion("VIA-1", 
                new CargaRemota(100, 1.0, "CAJAS"), 
                new CargaRemota(150, 1.5, "CAJAS"), 
                "ACEPTADA", 
                new ImporteRemoto("10.00", "PEN"), 
                OffsetDateTime.now());
        
        assertThatCode(() -> pasarela.reportarDiferencia("ORD-1", peticion)).doesNotThrowAnyException();
    }

    @Test
    void sinCabeceraOIncorrectaDevuelve400QueEsFalloDeIntegracion() {
        cuerpo = "{}";
        expectedIdempotencyKey = "CLAVE-QUE-NO-SE-ENVIARA"; 
        
        DiferenciaCargaPeticion peticion = new DiferenciaCargaPeticion("VIA-1", 
                new CargaRemota(100, 1.0, "CAJAS"), 
                new CargaRemota(150, 1.5, "CAJAS"), 
                "ACEPTADA", 
                new ImporteRemoto("10.00", "PEN"), 
                OffsetDateTime.now());
        
        assertThatThrownBy(() -> pasarela.reportarDiferencia("ORD-1", peticion))
                .isInstanceOf(ComercialIntegrationException.class)
                .hasMessageContaining("400");
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
