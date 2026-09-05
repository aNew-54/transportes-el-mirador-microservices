package pe.edu.unc.elmirador.cobranza.dto.internal.response;

import java.time.LocalDate;
import java.util.List;
import pe.edu.unc.elmirador.cobranza.dto.internal.request.ImporteRequest;

public record EstadoCrediticioResponse(
        String clienteId,
        String situacion,
        LocalDate fechaDeCambio,
        int diasDeAtrasoMaximo,
        int cuentasVencidas,
        List<ImporteRequest> deudaPorMoneda
) {
}
