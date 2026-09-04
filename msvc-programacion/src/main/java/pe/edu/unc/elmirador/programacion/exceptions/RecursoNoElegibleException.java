package pe.edu.unc.elmirador.programacion.exceptions;

import java.util.List;

public class RecursoNoElegibleException extends DominioProgramacionException {

    private final List<String> motivos;

    public RecursoNoElegibleException(String message) {
        super(message);
        this.motivos = List.of();
    }

    public RecursoNoElegibleException(List<String> motivos) {
        super("Recurso no elegible: " + (motivos != null ? String.join(", ", motivos) : ""));
        this.motivos = (motivos != null) ? List.copyOf(motivos) : List.of();
    }

    public RecursoNoElegibleException(String message, List<String> motivos) {
        super(message);
        this.motivos = (motivos != null) ? List.copyOf(motivos) : List.of();
    }

    public List<String> getMotivos() {
        return motivos;
    }
}
