package pe.edu.unc.elmirador.ejecucion.exceptions;

public class FacturacionIntegrationException extends RuntimeException {

    public FacturacionIntegrationException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public FacturacionIntegrationException(String mensaje) {
        super(mensaje);
    }
}
