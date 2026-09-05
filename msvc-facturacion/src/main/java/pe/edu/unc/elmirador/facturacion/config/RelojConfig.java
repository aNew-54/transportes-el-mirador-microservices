package pe.edu.unc.elmirador.facturacion.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RelojConfig {
    @Bean
    public Clock reloj() {
        return Clock.system(ZoneId.of("America/Lima"));
    }
}
