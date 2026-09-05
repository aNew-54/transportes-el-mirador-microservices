package pe.edu.unc.elmirador.cobranza.dto.response;

import java.time.LocalDate;
import java.util.List;

import pe.edu.unc.elmirador.cobranza.models.vo.SituacionCrediticia;

/**
 * Posicion deudora completa de un cliente.
 *
 * <p>{@code deudaPorMoneda} lleva un importe por cada moneda que el cliente realmente debe, y esta
 * vacia cuando no debe nada. No hay un «deudaTotal» escalar: {@code deudaTotal(codigoMoneda)} exige
 * la moneda justamente para que nadie la adivine, y un unico total obligaria a elegir una.
 */
public record CuentaCorrienteResponse(
        String clienteId,
        SituacionCrediticia situacion,
        String motivo,
        LocalDate fechaDeCambio,
        List<ImporteResponse> deudaPorMoneda,
        int diasDeAtrasoMaximo,
        int cuentasVencidas,
        List<CuentaPorCobrarResponse> cuentas
) {
}
