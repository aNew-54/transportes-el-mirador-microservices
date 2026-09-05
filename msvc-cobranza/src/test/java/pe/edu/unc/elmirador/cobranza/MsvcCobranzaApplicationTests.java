package pe.edu.unc.elmirador.cobranza;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import pe.edu.unc.elmirador.cobranza.controllers.CarteraController;
import pe.edu.unc.elmirador.cobranza.controllers.CuentaCorrienteController;
import pe.edu.unc.elmirador.cobranza.controllers.CuentaCorrienteInternalController;
import pe.edu.unc.elmirador.cobranza.controllers.ManejadorDeErrores;
import pe.edu.unc.elmirador.cobranza.controllers.PagoController;
import pe.edu.unc.elmirador.cobranza.repositories.CuentaCorrienteDelClienteRepository;
import pe.edu.unc.elmirador.cobranza.repositories.PagoRepository;
import pe.edu.unc.elmirador.cobranza.services.CuentaCorrienteService;
import pe.edu.unc.elmirador.cobranza.services.PagoService;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
        }
)
class MsvcCobranzaApplicationTests {

    @MockitoBean
    private CuentaCorrienteDelClienteRepository cuentaRepositorio;

    @MockitoBean
    private PagoRepository pagoRepositorio;

    @Autowired
    private CuentaCorrienteController cuentaCorrienteController;

    @Autowired
    private CuentaCorrienteInternalController cuentaCorrienteInternalController;

    @Autowired
    private PagoController pagoController;

    @Autowired
    private CarteraController carteraController;

    @Autowired
    private CuentaCorrienteService cuentaService;

    @Autowired
    private PagoService pagoService;

    @Autowired
    private ManejadorDeErrores manejadorDeErrores;

    @Test
    void elGrafoDeBeansEstaCompleto() {
        assertThat(cuentaCorrienteController).isNotNull();
        assertThat(cuentaCorrienteInternalController).isNotNull();
        assertThat(pagoController).isNotNull();
        assertThat(carteraController).isNotNull();
        assertThat(cuentaService).isNotNull();
        assertThat(pagoService).isNotNull();
        assertThat(manejadorDeErrores).isNotNull();
    }
}
