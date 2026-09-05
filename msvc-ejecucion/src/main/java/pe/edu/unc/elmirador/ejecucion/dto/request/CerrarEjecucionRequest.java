package pe.edu.unc.elmirador.ejecucion.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Lo que hace falta para cerrar un viaje y rendir cuentas a los otros cuatro contextos.
 *
 * <p>Aqui ya no viaja {@code hayLiquidacionesPendientes}. LIQ-04 se comprobaba contra ese booleano,
 * asi que bastaba mandar {@code false} para que la invariante no pudiera fallar nunca. Las
 * liquidaciones son de este contexto y {@code LiquidacionDeViajeRepository} sabe contarlas: la
 * cuenta la hace el servicio, no quien llama.
 *
 * <p>Lo que si viene del cuerpo son hechos que ningun contexto puede deducir —el odometro se lee
 * del tablero, las horas las firma el conductor, los importes los pone un tarifario que Ejecucion
 * no tiene—. Que sean del cuerpo no los hace sospechosos; lo sospechoso seria que viniera la
 * conclusion.
 */
public record CerrarEjecucionRequest(
        @NotNull @Positive Integer kilometrajeFinal,
        @NotEmpty @Valid List<HorasDeConductorRequest> horasPorConductor,
        @NotNull @Valid List<ConceptoFacturableRequest> conceptosFacturables
) {
}
