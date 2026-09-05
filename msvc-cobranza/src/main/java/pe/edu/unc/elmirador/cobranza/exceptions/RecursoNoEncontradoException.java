package pe.edu.unc.elmirador.cobranza.exceptions;

public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String recurso, String id) {
        super(String.format("No existe %s con id %s", recurso, id));
    }
}
