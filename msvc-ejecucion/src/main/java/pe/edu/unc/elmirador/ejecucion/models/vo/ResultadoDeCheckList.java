package pe.edu.unc.elmirador.ejecucion.models.vo;

import jakarta.persistence.Embeddable;
import java.time.OffsetDateTime;
import java.util.List;

@Embeddable
public record ResultadoDeCheckList(boolean aprobado, List<String> observaciones, OffsetDateTime momento) {

    public ResultadoDeCheckList {
        if (momento == null) {
            throw new IllegalArgumentException("El momento del check-list es obligatorio");
        }
        observaciones = observaciones != null ? List.copyOf(observaciones) : List.of();
        if (!aprobado && (observaciones.isEmpty() || observaciones.stream().allMatch(String::isBlank))) {
            throw new IllegalArgumentException("Un check-list no aprobado exige observaciones no vacias");
        }
    }

    public static ResultadoDeCheckList aprobado(OffsetDateTime momento) {
        return new ResultadoDeCheckList(true, List.of(), momento);
    }

    public static ResultadoDeCheckList noAprobado(List<String> observaciones, OffsetDateTime momento) {
        return new ResultadoDeCheckList(false, observaciones, momento);
    }
}
