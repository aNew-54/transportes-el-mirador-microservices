package pe.edu.unc.elmirador.unidades.exceptions;

public class DominioUnidadesException extends RuntimeException {

    public DominioUnidadesException(String mensaje) {
        super(mensaje);
    }

    public DominioUnidadesException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
