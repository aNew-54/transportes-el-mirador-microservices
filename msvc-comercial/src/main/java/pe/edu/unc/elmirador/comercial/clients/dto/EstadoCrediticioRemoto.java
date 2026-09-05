package pe.edu.unc.elmirador.comercial.clients.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Espejo de la respuesta del contrato 11 (Comercial -> Cobranza).
 *
 * <p>No es {@code dto/response/EstadoCrediticioResponse}, que es lo que este modulo publica. Son dos
 * formas que hoy se parecen y que no tienen por que seguir pareciendose: una la decide Comercial, la
 * otra la decide Cobranza.
 *
 * <p>Sostiene CLI-01 y ORD-02 solamente a traves de {@code situacion}. El resto de campos son
 * informativos y viajan porque el contrato los publica, no porque el dominio los necesite.
 */
public record EstadoCrediticioRemoto(
        String clienteId,
        String situacion,
        LocalDate fechaDeCambio,
        int diasDeAtrasoMaximo,
        int cuentasVencidas,
        List<ImporteRemoto> deudaPorMoneda
) {
}
