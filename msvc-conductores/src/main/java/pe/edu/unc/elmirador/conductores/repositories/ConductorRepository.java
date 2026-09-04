package pe.edu.unc.elmirador.conductores.repositories;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.unc.elmirador.conductores.models.entity.Conductor;
import pe.edu.unc.elmirador.conductores.models.vo.SituacionDeHabilitacion;

/**
 * Repositorio de la raiz de agregado. La induccion no tiene el suyo: se alcanza a traves del
 * conductor, que es lo que significa ser entidad hija.
 */
public interface ConductorRepository extends JpaRepository<Conductor, String> {

    Optional<Conductor> findByNumeroDeLicenciaValor(String valor);

    java.util.List<Conductor> findByEstadoSituacion(SituacionDeHabilitacion situacion);
}
