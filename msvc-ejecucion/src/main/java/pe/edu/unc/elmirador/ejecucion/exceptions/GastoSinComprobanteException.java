package pe.edu.unc.elmirador.ejecucion.exceptions;

public class GastoSinComprobanteException extends DominioEjecucionException {

    public GastoSinComprobanteException(String message) {
        super(message);
    }

    public GastoSinComprobanteException(String message, Throwable cause) {
        super(message, cause);
    }
}
