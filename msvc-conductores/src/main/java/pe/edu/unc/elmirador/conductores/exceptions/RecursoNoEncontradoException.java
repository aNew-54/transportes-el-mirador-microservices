package pe.edu.unc.elmirador.conductores.exceptions;

/**
 * El agregado pedido no existe. Se traduce a {@code 404}.
 *
 * <p>Extiende {@link RuntimeException} y no {@link DominioConductoresException} a proposito: no es
 * una regla de negocio, y heredar de la raiz del dominio la haria caer en el {@code 422} por defecto
 * del manejador de errores, que es justo el codigo equivocado.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso, String id) {
        super("No existe " + recurso + " con id " + id);
    }
}
