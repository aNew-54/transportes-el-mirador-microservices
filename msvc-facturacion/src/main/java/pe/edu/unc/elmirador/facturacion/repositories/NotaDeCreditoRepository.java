package pe.edu.unc.elmirador.facturacion.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.unc.elmirador.facturacion.models.entity.NotaDeCredito;

/** NotaDeCredito es raiz de agregado, no entidad hija de Factura: tiene su propio repositorio. */
public interface NotaDeCreditoRepository extends JpaRepository<NotaDeCredito, String> {

    List<NotaDeCredito> findByFacturaId(String facturaId);
}
