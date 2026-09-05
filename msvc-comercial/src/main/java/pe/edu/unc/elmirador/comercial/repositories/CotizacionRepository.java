package pe.edu.unc.elmirador.comercial.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.unc.elmirador.comercial.models.entity.Cotizacion;
import pe.edu.unc.elmirador.comercial.models.vo.EstadoDeCotizacion;

/**
 * Repositorio de la raiz de agregado Cotizacion.
 */
public interface CotizacionRepository extends JpaRepository<Cotizacion, String> {

    List<Cotizacion> findByClienteId(String clienteId);

    List<Cotizacion> findByEstado(EstadoDeCotizacion estado);

    List<Cotizacion> findByRutaOrigenAndRutaDestino(String origen, String destino);
}
