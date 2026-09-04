package pe.edu.unc.elmirador.facturacion.exceptions;

public class DominioFacturacionException extends RuntimeException {

    public DominioFacturacionException(String message) {
        super(message);
    }

    public DominioFacturacionException(String message, Throwable cause) {
        super(message, cause);
    }
}
