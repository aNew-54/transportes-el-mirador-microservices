package pe.edu.unc.elmirador.programacion.models.vo;

import jakarta.persistence.Embeddable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Embeddable
public class ElegibilidadDeRecurso {

    private boolean elegible;
    private List<String> motivos = new ArrayList<>();

    /** Exigido por JPA. No usar: no valida nada. */
    protected ElegibilidadDeRecurso() {
    }

    public ElegibilidadDeRecurso(boolean elegible, List<String> motivos) {
        if (motivos == null) {
            throw new IllegalArgumentException("La lista de motivos es obligatoria");
        }
        if (!elegible && motivos.isEmpty()) {
            throw new IllegalArgumentException("Un recurso no elegible exige motivos no vacios");
        }
        this.elegible = elegible;
        this.motivos.addAll(motivos);
    }

    public static ElegibilidadDeRecurso recursoElegible() {
        return new ElegibilidadDeRecurso(true, List.of());
    }

    public static ElegibilidadDeRecurso recursoNoElegible(String... motivos) {
        if (motivos == null) {
            throw new IllegalArgumentException("Los motivos son obligatorios");
        }
        return new ElegibilidadDeRecurso(false, List.of(motivos));
    }

    public static ElegibilidadDeRecurso recursoNoElegible(List<String> motivos) {
        return new ElegibilidadDeRecurso(false, motivos);
    }

    public boolean elegible() {
        return elegible;
    }

    public List<String> motivos() {
        return List.copyOf(motivos);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ElegibilidadDeRecurso otra)) return false;
        return elegible == otra.elegible
                && Objects.equals(motivos, otra.motivos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elegible, motivos);
    }

    @Override
    public String toString() {
        return "ElegibilidadDeRecurso[elegible=" + elegible + ", motivos=" + motivos + "]";
    }
}
