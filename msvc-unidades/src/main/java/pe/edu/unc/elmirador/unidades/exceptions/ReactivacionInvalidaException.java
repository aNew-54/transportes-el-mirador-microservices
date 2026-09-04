package pe.edu.unc.elmirador.unidades.exceptions;

/**
 * Se lanza al intentar devolver a servicio una unidad que todavia no cumple las condiciones
 * para operar: documentos vencidos (UNI-01) o mantenimiento preventivo vencido (UNI-02).
 */
public class ReactivacionInvalidaException extends DominioUnidadesException {

    public ReactivacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
