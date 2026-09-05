package pe.edu.unc.elmirador.comercial.dto.response;

import java.time.LocalDate;

public record EstadoCrediticioResponse(
        String situacion,
        LocalDate fechaDeCambio
) {
}
