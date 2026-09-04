package pe.edu.unc.elmirador.facturacion.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.unc.elmirador.facturacion.models.entity.Factura;
import pe.edu.unc.elmirador.facturacion.models.vo.EstadoDeFactura;

/**
 * FAC-02, la mitad que el dominio no puede sostener: el agregado garantiza que una factura
 * referencia una sola orden, pero la unicidad global la cierran este metodo y el indice unico
 * de la migracion.
 */
public interface FacturaRepository extends JpaRepository<Factura, String> {

    Optional<Factura> findByOrdenDeServicioId(String ordenDeServicioId);

    boolean existsByOrdenDeServicioId(String ordenDeServicioId);

    List<Factura> findByEstado(EstadoDeFactura estado);

    List<Factura> findByClienteId(String clienteId);
}
