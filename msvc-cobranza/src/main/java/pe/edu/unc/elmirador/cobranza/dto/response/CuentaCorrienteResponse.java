package pe.edu.unc.elmirador.cobranza.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import pe.edu.unc.elmirador.cobranza.models.vo.SituacionCrediticia;

public record CuentaCorrienteResponse(
        String clienteId,
        SituacionCrediticia situacion,
        String motivo,
        LocalDate fechaDeCambio,
        BigDecimal deudaTotalMonto,
        String deudaTotalMoneda,
        int diasDeAtrasoMaximo,
        int cuentasVencidas,
        List<CuentaPorCobrarResponse> cuentas
) {
}
