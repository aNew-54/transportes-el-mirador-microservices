package pe.edu.unc.elmirador.ejecucion.exceptions;

public class MonedaIncompatibleException extends DominioEjecucionException {

    public MonedaIncompatibleException(String message) {
        super(message);
    }

    public MonedaIncompatibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
