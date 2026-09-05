package pe.edu.unc.elmirador.programacion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import pe.edu.unc.elmirador.programacion.controllers.AgendaController;
import pe.edu.unc.elmirador.programacion.controllers.ManejadorDeErrores;
import pe.edu.unc.elmirador.programacion.controllers.ViajeController;
import pe.edu.unc.elmirador.programacion.controllers.ViajeInternalController;
import pe.edu.unc.elmirador.programacion.repositories.AgendaDeConductorRepository;
import pe.edu.unc.elmirador.programacion.repositories.AgendaDeUnidadRepository;
import pe.edu.unc.elmirador.programacion.repositories.ViajeRepository;
import pe.edu.unc.elmirador.programacion.services.AgendaService;
import pe.edu.unc.elmirador.programacion.services.ViajeService;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
        }
)
class MsvcProgramacionApplicationTests {

    @MockitoBean
    private ViajeRepository viajeRepository;

    @MockitoBean
    private AgendaDeUnidadRepository agendaDeUnidadRepository;

    @MockitoBean
    private AgendaDeConductorRepository agendaDeConductorRepository;

    @Autowired
    private ViajeController viajeController;

    @Autowired
    private ViajeInternalController viajeInternalController;

    @Autowired
    private AgendaController agendaController;

    @Autowired
    private ViajeService viajeService;

    @Autowired
    private AgendaService agendaService;

    @Autowired
    private ManejadorDeErrores manejadorDeErrores;

    @Test
    void elGrafoDeBeansEstaCompleto() {
        assertThat(viajeController).isNotNull();
        assertThat(viajeInternalController).isNotNull();
        assertThat(agendaController).isNotNull();
        assertThat(viajeService).isNotNull();
        assertThat(agendaService).isNotNull();
        assertThat(manejadorDeErrores).isNotNull();
    }
}
