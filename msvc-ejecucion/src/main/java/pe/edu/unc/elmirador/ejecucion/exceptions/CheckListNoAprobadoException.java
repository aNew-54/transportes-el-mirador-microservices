package pe.edu.unc.elmirador.ejecucion.exceptions;

public class CheckListNoAprobadoException extends DominioEjecucionException {

    public CheckListNoAprobadoException(String message) {
        super(message);
    }

    public CheckListNoAprobadoException(String message, Throwable cause) {
        super(message, cause);
    }
}
