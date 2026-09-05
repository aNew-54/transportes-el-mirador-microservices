package pe.edu.unc.elmirador.ejecucion.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El reloj de la aplicacion, inyectable.
 */
@Configuration
public class RelojConfig {

    /** Hora oficial del Peru. */
    @Bean
    public Clock reloj() {
        return Clock.system(ZoneId.of("America/Lima"));
    }
}
