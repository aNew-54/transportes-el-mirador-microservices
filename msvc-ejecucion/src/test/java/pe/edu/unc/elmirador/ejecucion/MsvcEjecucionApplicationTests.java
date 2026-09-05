package pe.edu.unc.elmirador.ejecucion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import pe.edu.unc.elmirador.ejecucion.controllers.EjecucionDeViajeController;
import pe.edu.unc.elmirador.ejecucion.controllers.LiquidacionDeViajeController;
import pe.edu.unc.elmirador.ejecucion.controllers.ManejadorDeErrores;
import pe.edu.unc.elmirador.ejecucion.repositories.EjecucionDeViajeRepository;
import pe.edu.unc.elmirador.ejecucion.repositories.LiquidacionDeViajeRepository;
import pe.edu.unc.elmirador.ejecucion.services.EjecucionDeViajeService;
import pe.edu.unc.elmirador.ejecucion.services.LiquidacionDeViajeService;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
        }
)
class MsvcEjecucionApplicationTests {

    @MockitoBean
    private EjecucionDeViajeRepository ejecucionRepository;

    @MockitoBean
    private LiquidacionDeViajeRepository liquidacionRepository;

    @Autowired
    private EjecucionDeViajeController ejecucionController;

    @Autowired
    private LiquidacionDeViajeController liquidacionController;

    @Autowired
    private EjecucionDeViajeService ejecucionService;

    @Autowired
    private LiquidacionDeViajeService liquidacionService;

    @Autowired
    private ManejadorDeErrores manejadorDeErrores;

    @Test
    void elGrafoDeBeansEstaCompleto() {
        assertThat(ejecucionController).isNotNull();
        assertThat(liquidacionController).isNotNull();
        assertThat(ejecucionService).isNotNull();
        assertThat(liquidacionService).isNotNull();
        assertThat(manejadorDeErrores).isNotNull();
    }
}
