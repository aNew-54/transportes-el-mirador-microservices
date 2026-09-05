package pe.edu.unc.elmirador.unidades;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import pe.edu.unc.elmirador.unidades.controllers.AlertaController;
import pe.edu.unc.elmirador.unidades.controllers.ManejadorDeErrores;
import pe.edu.unc.elmirador.unidades.controllers.OrdenDeMantenimientoController;
import pe.edu.unc.elmirador.unidades.controllers.RepuestoController;
import pe.edu.unc.elmirador.unidades.controllers.UnidadController;
import pe.edu.unc.elmirador.unidades.repositories.OrdenDeMantenimientoRepository;
import pe.edu.unc.elmirador.unidades.repositories.RepuestoRepository;
import pe.edu.unc.elmirador.unidades.repositories.UnidadRepository;
import pe.edu.unc.elmirador.unidades.services.OrdenDeMantenimientoService;
import pe.edu.unc.elmirador.unidades.services.RepuestoService;
import pe.edu.unc.elmirador.unidades.services.UnidadService;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
        }
)
class MsvcUnidadesApplicationTests {

    @MockitoBean
    private UnidadRepository unidadRepository;

    @MockitoBean
    private OrdenDeMantenimientoRepository ordenRepository;

    @MockitoBean
    private RepuestoRepository repuestoRepository;

    @Autowired
    private UnidadController unidadController;

    @Autowired
    private OrdenDeMantenimientoController ordenController;

    @Autowired
    private RepuestoController repuestoController;

    @Autowired
    private AlertaController alertaController;

    @Autowired
    private UnidadService unidadService;

    @Autowired
    private OrdenDeMantenimientoService ordenService;

    @Autowired
    private RepuestoService repuestoService;

    @Autowired
    private ManejadorDeErrores manejadorDeErrores;

    @Test
    void elGrafoDeBeansEstaCompleto() {
        assertThat(unidadController).isNotNull();
        assertThat(ordenController).isNotNull();
        assertThat(repuestoController).isNotNull();
        assertThat(alertaController).isNotNull();
        assertThat(unidadService).isNotNull();
        assertThat(ordenService).isNotNull();
        assertThat(repuestoService).isNotNull();
        assertThat(manejadorDeErrores).isNotNull();
    }
}
