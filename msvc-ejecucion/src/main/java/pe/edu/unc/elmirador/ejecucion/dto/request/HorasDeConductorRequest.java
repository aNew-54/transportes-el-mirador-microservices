package pe.edu.unc.elmirador.ejecucion.dto.request;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Las horas que un conductor firma al cerrar el viaje, tal como viajan en el contrato 6.
 *
 * <p>Vienen en el cuerpo a proposito: son un hecho observado, no un veredicto sobre una invariante.
 * Quien decide si superan el maximo normado es Conductores, con CON-02, y responde 409 si lo hacen.
 * Cero horas es valido y no es lo mismo que omitir al conductor: el conjunto reportado debe ser
 * exactamente el de conductores asignados.
 */
public record HorasDeConductorRequest(
        @NotBlank String conductorId,
        @NotNull @PositiveOrZero Double horas,
        @NotNull OffsetDateTime desde,
        @NotNull OffsetDateTime hasta
) {
}
