package pe.edu.unc.elmirador.programacion.exceptions;

public class ConductoresIntegrationException extends RuntimeException {
    public ConductoresIntegrationException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
    public ConductoresIntegrationException(String mensaje) {
        super(mensaje);
    }
}
