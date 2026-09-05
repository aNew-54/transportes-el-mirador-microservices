package pe.edu.unc.elmirador.cobranza.dto.response;

import java.math.BigDecimal;

/**
 * Un importe con su moneda (regla 6). Nunca viaja un monto suelto.
 */
public record ImporteResponse(BigDecimal monto, String moneda) {
}
