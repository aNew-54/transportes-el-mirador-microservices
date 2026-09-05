package pe.edu.unc.elmirador.facturacion.exceptions;

public class ComercialIntegrationException extends RuntimeException {
    public ComercialIntegrationException(String mensaje) {
        super(mensaje);
    }

    public ComercialIntegrationException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
