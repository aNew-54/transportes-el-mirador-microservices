package pe.edu.unc.elmirador.ejecucion.exceptions;

public class TransicionDeEjecucionInvalidaException extends DominioEjecucionException {

    public TransicionDeEjecucionInvalidaException(String message) {
        super(message);
    }

    public TransicionDeEjecucionInvalidaException(String message, Throwable cause) {
        super(message, cause);
    }
}
