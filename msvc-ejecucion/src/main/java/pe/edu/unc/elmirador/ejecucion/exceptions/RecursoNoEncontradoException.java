package pe.edu.unc.elmirador.ejecucion.exceptions;

/**
 * El agregado pedido no existe. Se traduce a {@code 404}.
 *
 * <p>Extiende {@link RuntimeException} a proposito.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso, String id) {
        super("No existe " + recurso + " con id " + id);
    }
}
