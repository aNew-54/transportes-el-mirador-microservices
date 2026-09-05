package pe.edu.unc.elmirador.facturacion;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import pe.edu.unc.elmirador.facturacion.controllers.FacturaController;
import pe.edu.unc.elmirador.facturacion.controllers.FacturacionInternalController;
import pe.edu.unc.elmirador.facturacion.controllers.NotaDeCreditoController;
import pe.edu.unc.elmirador.facturacion.controllers.ManejadorDeErrores;
import pe.edu.unc.elmirador.facturacion.repositories.FacturaRepository;
import pe.edu.unc.elmirador.facturacion.repositories.NotaDeCreditoRepository;
import pe.edu.unc.elmirador.facturacion.repositories.PeticionIdempotenteRepository;
import pe.edu.unc.elmirador.facturacion.services.FacturaService;
import pe.edu.unc.elmirador.facturacion.services.FacturacionInternalService;
import pe.edu.unc.elmirador.facturacion.services.NotaDeCreditoService;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
        }
)
class MsvcFacturacionApplicationTests {

    @MockitoBean private FacturaRepository facturaRepository;
    @MockitoBean private NotaDeCreditoRepository notaDeCreditoRepository;
    @MockitoBean private PeticionIdempotenteRepository peticionIdempotenteRepository;

    @Autowired private FacturaController facturaController;
    @Autowired private NotaDeCreditoController notaDeCreditoController;
    @Autowired private FacturacionInternalController facturacionInternalController;
    @Autowired private FacturaService facturaService;
    @Autowired private NotaDeCreditoService notaDeCreditoService;
    @Autowired private FacturacionInternalService facturacionInternalService;
    @Autowired private ManejadorDeErrores manejadorDeErrores;

    @Test
    void elGrafoDeBeansEstaCompleto() {
        assertThat(facturaController).isNotNull();
        assertThat(notaDeCreditoController).isNotNull();
        assertThat(facturacionInternalController).isNotNull();
        assertThat(facturaService).isNotNull();
        assertThat(notaDeCreditoService).isNotNull();
        assertThat(facturacionInternalService).isNotNull();
        assertThat(manejadorDeErrores).isNotNull();
    }
}
