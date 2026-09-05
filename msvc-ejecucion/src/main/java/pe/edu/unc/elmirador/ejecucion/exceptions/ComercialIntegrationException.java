package pe.edu.unc.elmirador.ejecucion.exceptions;

public class ComercialIntegrationException extends RuntimeException {

    public ComercialIntegrationException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public ComercialIntegrationException(String mensaje) {
        super(mensaje);
    }
}
