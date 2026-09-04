package pe.edu.unc.elmirador.conductores.exceptions;

public class DominioConductoresException extends RuntimeException {

    public DominioConductoresException(String message) {
        super(message);
    }

    public DominioConductoresException(String message, Throwable cause) {
        super(message, cause);
    }
}
