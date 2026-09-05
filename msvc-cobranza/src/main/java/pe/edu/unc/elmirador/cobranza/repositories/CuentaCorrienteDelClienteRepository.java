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

    /**
     * Localiza al titular a partir del id de una de sus cuentas por cobrar.
     *
     * <p>Es lo que permite que PAG-02 sea alcanzable. Buscar la cuenta dentro del titular que ya se
     * conoce garantiza que siempre sea suya, y entonces la invariante «un pago no puede aplicarse a
     * cuentas de un cliente distinto» no puede violarse nunca ni, por tanto, probarse. Aqui la cuenta
     * se encuentra viva donde este, y es el agregado {@code Pago} quien decide si la acepta.
     */
    Optional<CuentaCorrienteDelCliente> findByCuentasId(String cuentaId);
}
