package pe.edu.unc.elmirador.cobranza.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import pe.edu.unc.elmirador.cobranza.models.vo.ModalidadDePago;

public record PagoResponse(
        String id,
        String clienteId,
        BigDecimal montoMonto,
        String montoMoneda,
        ModalidadDePago modalidad,
        String referencia,
        LocalDate fecha,
        BigDecimal montoAplicadoMonto,
        String montoAplicadoMoneda,
        BigDecimal saldoSinAplicarMonto,
        String saldoSinAplicarMoneda,
        List<AplicacionDePagoResponse> aplicaciones
) {
}
