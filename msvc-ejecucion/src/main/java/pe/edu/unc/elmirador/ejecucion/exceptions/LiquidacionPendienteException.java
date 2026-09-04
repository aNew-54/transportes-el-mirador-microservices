package pe.edu.unc.elmirador.ejecucion.exceptions;

public class LiquidacionPendienteException extends DominioEjecucionException {

    public LiquidacionPendienteException(String message) {
        super(message);
    }

    public LiquidacionPendienteException(String message, Throwable cause) {
        super(message, cause);
    }
}
