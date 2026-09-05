package pe.edu.unc.elmirador.cobranza.dto.response;

import java.math.BigDecimal;

public record AplicacionDePagoResponse(
        String id,
        String cuentaPorCobrarId,
        BigDecimal importeMonto,
        String importeMoneda
) {
}
