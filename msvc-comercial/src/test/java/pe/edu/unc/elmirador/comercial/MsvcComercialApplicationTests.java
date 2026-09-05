package pe.edu.unc.elmirador.comercial;

import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import pe.edu.unc.elmirador.comercial.repositories.ClienteRepository;
import pe.edu.unc.elmirador.comercial.repositories.ContratoMarcoRepository;
import pe.edu.unc.elmirador.comercial.repositories.CotizacionRepository;
import pe.edu.unc.elmirador.comercial.repositories.OrdenDeServicioRepository;
import pe.edu.unc.elmirador.comercial.repositories.TarifarioRepository;

/**
 * Prueba de humo: verifica que el contexto de Spring arranca correctamente.
 *
 * <p>Para que la prueba sea util (regla 1), el contexto debe ser el real.
 * Excluir JPA sin reemplazar los repositorios dejaba el contexto a medias y
 * escondia fallos de inyeccion en los servicios.
 * Ahora se sustituyen con {@code @MockitoBean}, probando todo menos la BD.
 */
@SpringBootTest(
        classes = MsvcComercialApplication.class,
        properties = {
            "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
        }
)
class MsvcComercialApplicationTests {

    @MockitoBean
    private ClienteRepository clienteRepository;

    @MockitoBean
    private ContratoMarcoRepository contratoRepository;

    @MockitoBean
    private CotizacionRepository cotizacionRepository;

    @MockitoBean
    private OrdenDeServicioRepository ordenRepository;

    @MockitoBean
    private TarifarioRepository tarifarioRepository;

    @MockitoBean
    private pe.edu.unc.elmirador.comercial.repositories.PeticionIdempotenteRepository peticionIdempotenteRepository;

    @Autowired
    private pe.edu.unc.elmirador.comercial.controllers.ComercialInternalController comercialInternalController;

    @Autowired
    private pe.edu.unc.elmirador.comercial.services.ComercialInternalService comercialInternalService;

    @Test
    void contextLoads() {
        // Si llega aqui, el contexto arranco bien
    }
}
