package pe.edu.unc.elmirador.facturacion.dto.response;

/**
 * Resultado de una operacion idempotente.
 *
 * <p>{@code repetida} distingue el codigo de la primera llamada del codigo del reintento: el contrato
 * 10 devuelve {@code 201} la primera vez y {@code 200} despues. Sin este dato el controlador no puede
 * elegir, y devolver siempre el mismo codigo incumple el contrato.
 */
public record ResultadoIdempotente<T>(T cuerpo, boolean repetida) {
}
