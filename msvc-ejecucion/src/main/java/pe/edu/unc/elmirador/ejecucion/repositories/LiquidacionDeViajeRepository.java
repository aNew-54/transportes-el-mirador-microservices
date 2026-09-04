package pe.edu.unc.elmirador.ejecucion.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.unc.elmirador.ejecucion.models.entity.LiquidacionDeViaje;
import pe.edu.unc.elmirador.ejecucion.models.entity.LiquidacionDeViajeId;
import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoDeLiquidacion;

/**
 * LIQ-04 lo consulta el servicio de aplicacion: findByViajeIdAndEstadoNot dice si queda alguna
 * liquidacion pendiente antes de dejar cerrar la ejecucion.
 */
public interface LiquidacionDeViajeRepository extends JpaRepository<LiquidacionDeViaje, LiquidacionDeViajeId> {

    List<LiquidacionDeViaje> findByViajeId(String viajeId);

    List<LiquidacionDeViaje> findByViajeIdAndEstadoNot(String viajeId, EstadoDeLiquidacion estado);
}
