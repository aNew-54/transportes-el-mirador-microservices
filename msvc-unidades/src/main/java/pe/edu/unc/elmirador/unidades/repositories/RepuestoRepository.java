package pe.edu.unc.elmirador.unidades.repositories;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.unc.elmirador.unidades.models.entity.Repuesto;

/**
 * Repositorio de la raiz de agregado Repuesto.
 */
public interface RepuestoRepository extends JpaRepository<Repuesto, String> {

    Optional<Repuesto> findByCodigo(String codigo);
}
