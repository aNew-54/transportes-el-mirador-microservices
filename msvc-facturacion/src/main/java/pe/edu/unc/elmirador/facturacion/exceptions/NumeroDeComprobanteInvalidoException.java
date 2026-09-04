package pe.edu.unc.elmirador.facturacion.exceptions;

public class NumeroDeComprobanteInvalidoException extends DominioFacturacionException {

    public NumeroDeComprobanteInvalidoException(String message) {
        super(message);
    }

    public NumeroDeComprobanteInvalidoException(String message, Throwable cause) {
        super(message, cause);
    }
}
