package pe.edu.unc.elmirador.comercial.exceptions;

/**
 * Excepción genérica para cuando un recurso no existe.
 * Extiende RuntimeException a propósito para que el manejador la capture como 404
 * y no caiga en el comodín de 422 de las excepciones de dominio.
 */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String recurso, String id) {
        super("No existe " + recurso + " con ID " + id);
    }
}
