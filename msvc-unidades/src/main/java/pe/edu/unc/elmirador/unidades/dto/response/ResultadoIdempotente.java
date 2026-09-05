package pe.edu.unc.elmirador.unidades.dto.response;

/**
 * Resultado de una operacion idempotente.
 *
 * <p>{@code repetida} distingue el codigo de la primera llamada del codigo del reintento.
 */
public record ResultadoIdempotente<T>(T cuerpo, boolean repetida) {
}
