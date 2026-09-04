package pe.edu.unc.elmirador.unidades.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.unc.elmirador.unidades.models.entity.Unidad;
import pe.edu.unc.elmirador.unidades.models.vo.SituacionOperativa;

/**
 * Repositorio de la raiz de agregado Unidad. El documento vehicular no tiene el suyo:
 * se alcanza a traves de la unidad, que es lo que significa ser entidad hija.
 */
public interface UnidadRepository extends JpaRepository<Unidad, String> {

    Optional<Unidad> findByPlacaValor(String valor);

    List<Unidad> findByEstadoOperativoSituacion(SituacionOperativa situacion);
}
