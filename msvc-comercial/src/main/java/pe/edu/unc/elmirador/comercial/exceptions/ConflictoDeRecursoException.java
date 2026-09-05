package pe.edu.unc.elmirador.comercial.exceptions;

/**
 * Excepción genérica para conflictos de estado o unicidad que impiden una operación.
 * Extiende RuntimeException a propósito para que el manejador la capture como 409
 * y no caiga en el comodín de 422 de las excepciones de dominio.
 */
public class ConflictoDeRecursoException extends RuntimeException {
    public ConflictoDeRecursoException(String mensaje) {
        super(mensaje);
    }
}
