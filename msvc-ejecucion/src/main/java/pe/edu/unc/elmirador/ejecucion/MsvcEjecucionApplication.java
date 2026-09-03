package pe.edu.unc.elmirador.ejecucion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class MsvcEjecucionApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsvcEjecucionApplication.class, args);
    }
}
