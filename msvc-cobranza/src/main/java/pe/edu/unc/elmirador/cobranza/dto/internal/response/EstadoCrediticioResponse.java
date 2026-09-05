package pe.edu.unc.elmirador.cobranza.dto.internal.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Contrato 11 · Comercial → Cobranza. Sostiene CLI-01 y ORD-02.
 *
 * <p>{@code deudaPorMoneda} lleva un importe por cada moneda con deuda viva, y va vacia cuando el
 * cliente no debe nada. Un unico total obligaria a convertir a un tipo de cambio que este contexto no
 * conoce, y elegir una moneda por defecto es el defecto que este contexto ya corrigio dos veces.
 */
public record EstadoCrediticioResponse(
        String clienteId,
        String situacion,
        LocalDate fechaDeCambio,
        int diasDeAtrasoMaximo,
        int cuentasVencidas,
        List<ImporteResponse> deudaPorMoneda
) {
}
