package pe.edu.unc.elmirador.ejecucion.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoDeLiquidacion;

public record LiquidacionDeViajeResponse(
        String viajeId,
        String conductorId,
        BigDecimal anticipoMonto,
        String anticipoMoneda,
        List<GastoDeRutaResponse> gastos,
        EstadoDeLiquidacion estado,
        OffsetDateTime fechaDeAprobacion,
        BigDecimal saldoMonto,
        String saldoMoneda,
        String saldoSigno
) {
}
