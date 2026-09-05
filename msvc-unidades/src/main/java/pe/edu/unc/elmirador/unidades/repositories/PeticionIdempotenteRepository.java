package pe.edu.unc.elmirador.unidades.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.unc.elmirador.unidades.models.entity.PeticionIdempotente;

/** Memoria de los {@code POST} de integracion ya atendidos. Ver la regla 6 de los contratos. */
public interface PeticionIdempotenteRepository extends JpaRepository<PeticionIdempotente, String> {
}
