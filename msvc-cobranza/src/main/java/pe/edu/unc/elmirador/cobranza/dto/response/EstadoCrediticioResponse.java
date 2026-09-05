package pe.edu.unc.elmirador.cobranza.dto.response;

import java.time.LocalDate;

import pe.edu.unc.elmirador.cobranza.models.vo.SituacionCrediticia;

public record EstadoCrediticioResponse(
        SituacionCrediticia situacion,
        String motivo,
        LocalDate fechaDeCambio
) {
}
