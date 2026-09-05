package pe.edu.unc.elmirador.comercial.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.unc.elmirador.comercial.models.entity.PeticionIdempotente;

/** Memoria de los {@code POST} de integracion ya atendidos. Ver la regla 6 de los contratos. */
public interface PeticionIdempotenteRepository extends JpaRepository<PeticionIdempotente, String> {
}
