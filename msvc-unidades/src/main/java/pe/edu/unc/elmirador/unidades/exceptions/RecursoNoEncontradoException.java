package pe.edu.unc.elmirador.unidades.exceptions;

/**
 * El agregado pedido no existe. Se traduce a {@code 404}.
 *
 * <p>Extiende {@link RuntimeException} y no {@link DominioUnidadesException} a propósito: no es
 * una regla de negocio, y heredar de la raíz del dominio la haría caer en el {@code 422} por defecto
 * del manejador de errores, que es justo el código equivocado.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso, String id) {
        super("No existe " + recurso + " con id " + id);
    }
}
