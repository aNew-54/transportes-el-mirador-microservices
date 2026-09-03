package pe.edu.unc.elmirador.cobranza;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class MsvcCobranzaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsvcCobranzaApplication.class, args);
    }
}
