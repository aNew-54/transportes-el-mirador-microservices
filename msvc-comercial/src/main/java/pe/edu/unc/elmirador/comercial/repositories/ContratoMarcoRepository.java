package pe.edu.unc.elmirador.comercial.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.unc.elmirador.comercial.models.entity.ContratoMarco;

/**
 * Repositorio de la raiz de agregado ContratoMarco.
 */
public interface ContratoMarcoRepository extends JpaRepository<ContratoMarco, String> {

    List<ContratoMarco> findByClienteId(String clienteId);

    List<ContratoMarco> findByClausulaDeConsolidacionPermitida(boolean permitida);
}
