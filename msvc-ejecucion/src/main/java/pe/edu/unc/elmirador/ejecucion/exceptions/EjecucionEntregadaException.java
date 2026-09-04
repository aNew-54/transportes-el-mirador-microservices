package pe.edu.unc.elmirador.ejecucion.exceptions;

public class EjecucionEntregadaException extends DominioEjecucionException {

    public EjecucionEntregadaException(String message) {
        super(message);
    }

    public EjecucionEntregadaException(String message, Throwable cause) {
        super(message, cause);
    }
}
