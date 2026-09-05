package pe.edu.unc.elmirador.programacion.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.unc.elmirador.programacion.models.entity.AgendaDeConductor;

public interface AgendaDeConductorRepository extends JpaRepository<AgendaDeConductor, String> {
}
