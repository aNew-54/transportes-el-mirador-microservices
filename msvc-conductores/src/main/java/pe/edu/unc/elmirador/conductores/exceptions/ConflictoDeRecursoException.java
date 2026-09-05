package pe.edu.unc.elmirador.conductores.exceptions;

/**
 * Ya existe otro agregado con esa identidad natural. Se traduce a {@code 409}.
 *
 * <p>La unicidad no cabe dentro del agregado porque exige mirar a los demas, asi que la comprueba el
 * servicio de aplicacion contra el repositorio. No hereda de {@link DominioConductoresException} por
 * la misma razon que {@link RecursoNoEncontradoException}.
 */
public class ConflictoDeRecursoException extends RuntimeException {

    public ConflictoDeRecursoException(String message) {
        super(message);
    }
}
