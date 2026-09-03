package pe.edu.unc.elmirador.facturacion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class MsvcFacturacionApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsvcFacturacionApplication.class, args);
    }
}
