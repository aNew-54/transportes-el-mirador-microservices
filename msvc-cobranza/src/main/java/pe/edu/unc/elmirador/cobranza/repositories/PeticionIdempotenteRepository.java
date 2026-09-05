package pe.edu.unc.elmirador.cobranza.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.unc.elmirador.cobranza.models.entity.PeticionIdempotente;

public interface PeticionIdempotenteRepository extends JpaRepository<PeticionIdempotente, String> {
}
