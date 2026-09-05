package pe.edu.unc.elmirador.comercial.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.unc.elmirador.comercial.models.entity.OrdenDeServicio;
import pe.edu.unc.elmirador.comercial.models.vo.EstadoDeOrden;
import pe.edu.unc.elmirador.comercial.models.vo.ModalidadDePago;

/**
 * Repositorio de la raiz de agregado OrdenDeServicio.
 */
public interface OrdenDeServicioRepository extends JpaRepository<OrdenDeServicio, String> {

    List<OrdenDeServicio> findByClienteId(String clienteId);

    List<OrdenDeServicio> findByEstado(EstadoDeOrden estado);

    List<OrdenDeServicio> findByCondicionDePagoModalidad(ModalidadDePago modalidad);
}
