package pe.edu.unc.elmirador.cobranza.dto.internal.response;

/**
 * Un importe con su moneda.
 *
 * <p>{@code monto} es texto, no numero, porque la regla 2 de los contratos lo escribe entre comillas:
 * {@code { "monto": "1250.00", "moneda": "PEN" }}. Serializado como {@code BigDecimal}, JSON borra el
 * cero final y {@code 5420.30} llega como {@code 5420.3}: el mismo valor, pero no la misma escala, y
 * un importe sin sus dos decimales deja de parecer un importe.
 */
public record ImporteResponse(String monto, String moneda) {
}
