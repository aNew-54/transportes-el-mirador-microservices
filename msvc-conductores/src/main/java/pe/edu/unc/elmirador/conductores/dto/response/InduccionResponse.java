package pe.edu.unc.elmirador.conductores.dto.response;

import java.time.LocalDate;

/** Induccion vigente de un conductor para un cliente concreto. */
public record InduccionResponse(
        String id,
        String clienteId,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta
) {
}
