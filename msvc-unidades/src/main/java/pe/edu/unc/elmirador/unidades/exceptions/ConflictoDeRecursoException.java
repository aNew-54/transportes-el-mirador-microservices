package pe.edu.unc.elmirador.unidades.exceptions;

/**
 * Ya existe otro agregado con esa identidad natural. Se traduce a {@code 409}.
 *
 * <p>La unicidad no cabe dentro del agregado porque exige mirar a los demás, así que la comprueba el
 * servicio de aplicación contra el repositorio. No hereda de {@link DominioUnidadesException} por
 * la misma razón que {@link RecursoNoEncontradoException}.
 */
public class ConflictoDeRecursoException extends RuntimeException {

    public ConflictoDeRecursoException(String message) {
        super(message);
    }
}
