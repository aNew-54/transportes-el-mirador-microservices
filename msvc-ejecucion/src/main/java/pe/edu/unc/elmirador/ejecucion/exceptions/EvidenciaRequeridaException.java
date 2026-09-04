package pe.edu.unc.elmirador.ejecucion.exceptions;

public class EvidenciaRequeridaException extends DominioEjecucionException {

    public EvidenciaRequeridaException(String message) {
        super(message);
    }

    public EvidenciaRequeridaException(String message, Throwable cause) {
        super(message, cause);
    }
}
