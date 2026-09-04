package pe.edu.unc.elmirador.cobranza.repositories;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.unc.elmirador.cobranza.models.entity.Pago;

/**
 * Repositorio de la raiz de agregado Pago.
 * La aplicacion de pago no tiene el suyo: se alcanza a traves del pago,
 * que es lo que significa ser entidad hija.
 */
public interface PagoRepository extends JpaRepository<Pago, String> {

    List<Pago> findByClienteId(String clienteId);

    List<Pago> findByFecha(LocalDate fecha);
}
