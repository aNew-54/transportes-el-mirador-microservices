package pe.edu.unc.elmirador.comercial.clients.dto;

/**
 * Un importe tal como lo escribe un contrato: monto en texto y codigo de moneda.
 *
 * <p>{@code monto} es {@code String} porque la regla 2 de {@code contracts.md} lo publica entre
 * comillas. Decodificarlo como {@code BigDecimal} funcionaria, pero este record es el espejo del JSON
 * ajeno y su trabajo es parecerse al JSON, no interpretarlo. La conversion es del gateway.
 */
public record ImporteRemoto(String monto, String moneda) {
}
