package pe.edu.unc.elmirador.unidades.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.unc.elmirador.unidades.models.entity.OrdenDeMantenimiento;
import pe.edu.unc.elmirador.unidades.models.vo.EstadoDeOrden;

/**
 * Repositorio de la raiz de agregado OrdenDeMantenimiento. El trabajo realizado no tiene el suyo:
 * se alcanza a traves de la orden, que es lo que significa ser entidad hija.
 */
public interface OrdenDeMantenimientoRepository extends JpaRepository<OrdenDeMantenimiento, String> {

    List<OrdenDeMantenimiento> findByUnidadId(String unidadId);

    List<OrdenDeMantenimiento> findByEstado(EstadoDeOrden estado);
}
