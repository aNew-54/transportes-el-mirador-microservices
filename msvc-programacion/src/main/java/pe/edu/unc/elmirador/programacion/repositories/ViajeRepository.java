package pe.edu.unc.elmirador.programacion.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.unc.elmirador.programacion.models.entity.Viaje;
import pe.edu.unc.elmirador.programacion.models.vo.EstadoDeViaje;

public interface ViajeRepository extends JpaRepository<Viaje, String> {

    List<Viaje> findByEstado(EstadoDeViaje estado);

    List<Viaje> findByRutaCorredor(String corredor);

    List<Viaje> findByAsignacionDeRecursosUnidadId(String unidadId);
}
