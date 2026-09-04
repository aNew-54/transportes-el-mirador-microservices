package pe.edu.unc.elmirador.programacion.models.vo;

import jakarta.persistence.Embeddable;
import java.util.List;

@Embeddable
public record ElegibilidadDeRecurso(boolean elegible, List<String> motivos) {

    public ElegibilidadDeRecurso {
        if (motivos == null) {
            throw new IllegalArgumentException("La lista de motivos es obligatoria");
        }
        motivos = List.copyOf(motivos);
        if (!elegible && motivos.isEmpty()) {
            throw new IllegalArgumentException("Un recurso no elegible exige motivos no vacios");
        }
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
}
