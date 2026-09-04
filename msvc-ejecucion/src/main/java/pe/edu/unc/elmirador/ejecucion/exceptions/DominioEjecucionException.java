package pe.edu.unc.elmirador.ejecucion.exceptions;

public class DominioEjecucionException extends RuntimeException {

    public DominioEjecucionException(String message) {
        super(message);
    }

    public DominioEjecucionException(String message, Throwable cause) {
        super(message, cause);
    }
}
