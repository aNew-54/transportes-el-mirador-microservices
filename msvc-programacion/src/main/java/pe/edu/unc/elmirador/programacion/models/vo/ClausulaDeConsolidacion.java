package pe.edu.unc.elmirador.programacion.models.vo;

import jakarta.persistence.Embeddable;
import java.util.List;

@Embeddable
public record ClausulaDeConsolidacion(boolean permitida, List<String> restricciones) {

    public ClausulaDeConsolidacion {
        if (restricciones == null) {
            throw new IllegalArgumentException("La lista de restricciones es obligatoria");
        }
        restricciones = List.copyOf(restricciones);
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
}
