package pe.edu.unc.elmirador.ejecucion.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.unc.elmirador.ejecucion.models.entity.EjecucionDeViaje;
import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoDeEjecucion;

/** La identidad de la ejecucion es el viajeId: comparte identidad con el viaje planificado. */
public interface EjecucionDeViajeRepository extends JpaRepository<EjecucionDeViaje, String> {

    List<EjecucionDeViaje> findByEstado(EstadoDeEjecucion estado);

    List<EjecucionDeViaje> findByUnidadEjecutoraId(String unidadEjecutoraId);
}
