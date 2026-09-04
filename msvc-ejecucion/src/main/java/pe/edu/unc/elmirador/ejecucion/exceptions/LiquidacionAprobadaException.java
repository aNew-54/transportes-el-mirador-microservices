package pe.edu.unc.elmirador.ejecucion.exceptions;

public class LiquidacionAprobadaException extends DominioEjecucionException {

    public LiquidacionAprobadaException(String message) {
        super(message);
    }

    public LiquidacionAprobadaException(String message, Throwable cause) {
        super(message, cause);
    }
}
