package pe.edu.unc.elmirador.comercial.repositories;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.unc.elmirador.comercial.models.entity.Tarifario;

/**
 * Repositorio de la raiz de agregado Tarifario.
 */
public interface TarifarioRepository extends JpaRepository<Tarifario, String> {

    List<Tarifario> findByVigenciaHastaGreaterThanEqual(LocalDate fecha);
}
