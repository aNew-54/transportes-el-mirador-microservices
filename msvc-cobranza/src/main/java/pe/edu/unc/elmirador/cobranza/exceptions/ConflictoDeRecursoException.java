package pe.edu.unc.elmirador.cobranza.exceptions;

public class ConflictoDeRecursoException extends RuntimeException {
    public ConflictoDeRecursoException(String mensaje) {
        super(mensaje);
    }
}
