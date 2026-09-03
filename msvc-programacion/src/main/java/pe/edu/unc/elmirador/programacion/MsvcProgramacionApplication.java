package pe.edu.unc.elmirador.programacion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class MsvcProgramacionApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsvcProgramacionApplication.class, args);
    }
}
