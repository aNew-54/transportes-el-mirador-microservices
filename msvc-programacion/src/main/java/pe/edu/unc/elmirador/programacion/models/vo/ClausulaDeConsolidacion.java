package pe.edu.unc.elmirador.programacion.models.vo;

import jakarta.persistence.Embeddable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Embeddable
public class ClausulaDeConsolidacion {

    private boolean permitida;
    private List<String> restricciones = new ArrayList<>();

    /** Exigido por JPA. No usar: no valida nada. */
    protected ClausulaDeConsolidacion() {
    }

    public ClausulaDeConsolidacion(boolean permitida, List<String> restricciones) {
        if (restricciones == null) {
            throw new IllegalArgumentException("La lista de restricciones es obligatoria");
        }
        this.permitida = permitida;
        this.restricciones.addAll(restricciones);
    }

    public static ClausulaDeConsolidacion consolidacionPermitida() {
        return new ClausulaDeConsolidacion(true, List.of());
    }

    public static ClausulaDeConsolidacion consolidacionPermitida(List<String> restricciones) {
        return new ClausulaDeConsolidacion(true, restricciones);
    }

    public static ClausulaDeConsolidacion consolidacionProhibida(List<String> restricciones) {
        return new ClausulaDeConsolidacion(false, restricciones);
    }

    public boolean permitida() {
        return permitida;
    }

    public List<String> restricciones() {
        return List.copyOf(restricciones);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClausulaDeConsolidacion otra)) return false;
        return permitida == otra.permitida
                && Objects.equals(restricciones, otra.restricciones);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permitida, restricciones);
    }

    @Override
    public String toString() {
        return "ClausulaDeConsolidacion[permitida=" + permitida + ", restricciones=" + restricciones + "]";
    }
}
