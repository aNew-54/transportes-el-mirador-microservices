package pe.edu.unc.elmirador.ejecucion.exceptions;

public class ProgramacionIntegrationException extends RuntimeException {

    public ProgramacionIntegrationException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public ProgramacionIntegrationException(String mensaje) {
        super(mensaje);
    }
}
