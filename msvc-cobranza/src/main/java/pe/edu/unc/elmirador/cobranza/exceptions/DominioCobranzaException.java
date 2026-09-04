package pe.edu.unc.elmirador.cobranza.exceptions;

public class DominioCobranzaException extends RuntimeException {

    public DominioCobranzaException(String message) {
        super(message);
    }

    public DominioCobranzaException(String message, Throwable cause) {
        super(message, cause);
    }
}
