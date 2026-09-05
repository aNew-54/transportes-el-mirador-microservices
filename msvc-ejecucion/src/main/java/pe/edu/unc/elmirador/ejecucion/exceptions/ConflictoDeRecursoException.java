package pe.edu.unc.elmirador.ejecucion.exceptions;

/**
 * Ya existe otro agregado con esa identidad natural. Se traduce a {@code 409}.
 */
public class ConflictoDeRecursoException extends RuntimeException {

    public ConflictoDeRecursoException(String message) {
        super(message);
    }
}
