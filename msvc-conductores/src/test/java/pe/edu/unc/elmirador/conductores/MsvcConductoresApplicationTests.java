package pe.edu.unc.elmirador.conductores;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import pe.edu.unc.elmirador.conductores.controllers.ConductorController;
import pe.edu.unc.elmirador.conductores.controllers.ManejadorDeErrores;
import pe.edu.unc.elmirador.conductores.repositories.ConductorRepository;
import pe.edu.unc.elmirador.conductores.services.ConductorService;

/**
 * Comprueba que el grafo de beans esta completo sin necesidad de Docker.
 *
 * <p>Excluye la fuente de datos y sustituye el repositorio, que es lo unico que exige una base real.
 * Lo que queda sigue siendo el grafo de verdad: si alguien olvida una anotacion {@code @Service} o
 * {@code @RestControllerAdvice}, o pide por constructor un bean que nadie declara, esta prueba falla
 * en segundos. Levantar MySQL de verdad es trabajo de {@code PersistenciaConductoresIT}.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
        }
)
class MsvcConductoresApplicationTests {

    @MockitoBean
    private ConductorRepository repositorio;

    @Autowired
    private ConductorController controlador;

    @Autowired
    private ConductorService servicio;

    @Autowired
    private ManejadorDeErrores manejadorDeErrores;

    @Test
    void elGrafoDeBeansEstaCompleto() {
        assertThat(controlador).isNotNull();
        assertThat(servicio).isNotNull();
        assertThat(manejadorDeErrores).isNotNull();
    }
}
