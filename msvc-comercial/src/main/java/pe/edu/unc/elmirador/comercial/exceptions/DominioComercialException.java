package pe.edu.unc.elmirador.comercial.exceptions;

public class DominioComercialException extends RuntimeException {

    public DominioComercialException(String message) {
        super(message);
    }

    public DominioComercialException(String message, Throwable cause) {
        super(message, cause);
    }
}
