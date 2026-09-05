package pe.edu.unc.elmirador.conductores.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El reloj de la aplicacion, inyectable.
 *
 * <p>Regla D1: el dominio no lee el reloj. Quien lo lee es el servicio de aplicacion, y lo recibe
 * inyectado para que una prueba pueda fijarlo con {@link Clock#fixed}. Un {@code LocalDate.now()}
 * sin argumento en cualquier punto de este modulo es un defecto.
 */
@Configuration
public class RelojConfig {

    /** Hora oficial del Peru. Las fechas de este contexto son dias de calendario peruano. */
    @Bean
    public Clock reloj() {
        return Clock.system(ZoneId.of("America/Lima"));
    }
}
