package pe.edu.unc.elmirador.comercial;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class MsvcComercialApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsvcComercialApplication.class, args);
    }
}
