package pe.edu.unc.elmirador.unidades;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class MsvcUnidadesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsvcUnidadesApplication.class, args);
    }
}
