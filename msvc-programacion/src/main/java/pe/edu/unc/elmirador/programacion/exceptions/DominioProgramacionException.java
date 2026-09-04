package pe.edu.unc.elmirador.programacion.exceptions;

public class DominioProgramacionException extends RuntimeException {

    public DominioProgramacionException(String message) {
        super(message);
    }

    public DominioProgramacionException(String message, Throwable cause) {
        super(message, cause);
    }
}
