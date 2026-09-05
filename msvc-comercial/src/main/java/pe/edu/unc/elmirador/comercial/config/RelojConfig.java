package pe.edu.unc.elmirador.comercial.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El reloj de la aplicación, inyectable.
 *
 * <p>Regla D1: el dominio no lee el reloj. Quien lo lee es el servicio de aplicación, y lo recibe
 * inyectado para que una prueba pueda fijarlo con {@link Clock#fixed}. Un {@code LocalDate.now()}
 * sin argumento en cualquier punto de este módulo es un defecto.
 */
@Configuration
public class RelojConfig {

    /** Hora oficial del Perú. Las fechas de este contexto son días de calendario peruano. */
    @Bean
    public Clock reloj() {
        return Clock.system(ZoneId.of("America/Lima"));
    }
}
