package pe.edu.unc.elmirador.programacion.exceptions;

public class UnidadesIntegrationException extends RuntimeException {
    public UnidadesIntegrationException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
    public UnidadesIntegrationException(String mensaje) {
        super(mensaje);
    }
}
