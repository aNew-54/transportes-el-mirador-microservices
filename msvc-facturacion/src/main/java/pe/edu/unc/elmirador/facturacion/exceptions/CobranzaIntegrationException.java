package pe.edu.unc.elmirador.facturacion.exceptions;

public class CobranzaIntegrationException extends RuntimeException {
    public CobranzaIntegrationException(String mensaje) {
        super(mensaje);
    }

    public CobranzaIntegrationException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
