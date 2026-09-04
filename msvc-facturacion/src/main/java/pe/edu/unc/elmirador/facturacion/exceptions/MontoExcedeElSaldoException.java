package pe.edu.unc.elmirador.facturacion.exceptions;

public class MontoExcedeElSaldoException extends DominioFacturacionException {

    public MontoExcedeElSaldoException(String message) {
        super(message);
    }
}
