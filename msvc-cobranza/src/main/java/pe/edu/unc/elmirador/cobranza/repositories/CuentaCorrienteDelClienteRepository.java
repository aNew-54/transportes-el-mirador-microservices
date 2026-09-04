package pe.edu.unc.elmirador.cobranza.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.unc.elmirador.cobranza.models.entity.CuentaCorrienteDelCliente;
import pe.edu.unc.elmirador.cobranza.models.vo.SituacionCrediticia;

/**
 * Repositorio de la raiz de agregado CuentaCorrienteDelCliente.
 * La cuenta por cobrar no tiene el suyo: se alcanza a traves de la cuenta corriente,
 * que es lo que significa ser entidad hija.
 */
public interface CuentaCorrienteDelClienteRepository extends JpaRepository<CuentaCorrienteDelCliente, String> {

    Optional<CuentaCorrienteDelCliente> findByClienteId(String clienteId);

    List<CuentaCorrienteDelCliente> findByEstadoSituacion(SituacionCrediticia situacion);
}
