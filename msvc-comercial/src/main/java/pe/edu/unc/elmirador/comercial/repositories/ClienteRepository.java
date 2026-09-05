package pe.edu.unc.elmirador.comercial.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.unc.elmirador.comercial.models.entity.Cliente;
import pe.edu.unc.elmirador.comercial.models.vo.ModalidadDePago;
import pe.edu.unc.elmirador.comercial.models.vo.SituacionCrediticia;

/**
 * Repositorio de la raiz de agregado Cliente.
 */
public interface ClienteRepository extends JpaRepository<Cliente, String> {

    Optional<Cliente> findByRucValor(String valor);

    List<Cliente> findByEstadoCrediticioSituacion(SituacionCrediticia situacion);

    List<Cliente> findByCondicionHabitualModalidad(ModalidadDePago modalidad);
}
