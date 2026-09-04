package pe.edu.unc.elmirador.comercial.models.vo;

import jakarta.persistence.Embeddable;
import java.util.List;

/**
 * Objeto de valor inmutable que expresa si la carga asociada puede consolidarse y bajo que restricciones.
 * Es lo que viaja en el contrato 1 y sostiene VIA-04.
 */
@Embeddable
public record ClausulaDeConsolidacion(boolean permitida, List<String> restricciones) {

    public ClausulaDeConsolidacion {
        if (restricciones == null) {
            throw new IllegalArgumentException("La lista de restricciones no puede ser nula");
        }
        restricciones = List.copyOf(restricciones);
    }

    public static ClausulaDeConsolidacion permitida(List<String> restricciones) {
        return new ClausulaDeConsolidacion(true, restricciones);
    }

    public static ClausulaDeConsolidacion noPermitida() {
        return new ClausulaDeConsolidacion(false, List.of());
    }
}
