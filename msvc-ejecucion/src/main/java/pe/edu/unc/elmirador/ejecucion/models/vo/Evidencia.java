package pe.edu.unc.elmirador.ejecucion.models.vo;

import jakarta.persistence.Embeddable;
import java.time.OffsetDateTime;
import java.util.List;

@Embeddable
public record Evidencia(List<String> fotografias, String descripcion, OffsetDateTime momento) {

    public Evidencia {
        if (fotografias == null || fotografias.isEmpty()) {
            throw new IllegalArgumentException("La evidencia debe contener al menos una fotografia");
        }
        if (fotografias.stream().anyMatch(f -> f == null || f.isBlank())) {
            throw new IllegalArgumentException("Las fotografias no pueden contener elementos nulos o vacios");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripcion de la evidencia es obligatoria");
        }
        if (momento == null) {
            throw new IllegalArgumentException("El momento de la evidencia es obligatorio");
        }
        fotografias = List.copyOf(fotografias);
        descripcion = descripcion.trim();
    }
}
