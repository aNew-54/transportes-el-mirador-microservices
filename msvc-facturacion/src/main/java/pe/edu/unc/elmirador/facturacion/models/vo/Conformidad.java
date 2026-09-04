package pe.edu.unc.elmirador.facturacion.models.vo;

import jakarta.persistence.Embeddable;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Objeto de valor inmutable que representa la conformidad de entrega recibida desde Ejecucion (contrato 8).
 * Lista de incidencias inmutable, nunca nula. bloqueaEmision() cuando no esta registrada o tiene incidencias.
 */
@Embeddable
public record Conformidad(
    boolean registrada,
    List<String> incidenciasSinResolver,
    OffsetDateTime recibidaEn
) {
    public Conformidad {
        if (incidenciasSinResolver == null) {
            throw new IllegalArgumentException("La lista de incidencias sin resolver es obligatoria y no puede ser nula");
        }
        if (registrada && recibidaEn == null) {
            throw new IllegalArgumentException("El instante de recepcion es obligatorio cuando la conformidad esta registrada");
        }
        incidenciasSinResolver = List.copyOf(incidenciasSinResolver);
    }

    public static Conformidad noRegistrada() {
        return new Conformidad(false, List.of(), null);
    }

    public static Conformidad conforme(OffsetDateTime recibidaEn) {
        return new Conformidad(true, List.of(), recibidaEn);
    }

    public static Conformidad conIncidencias(List<String> incidenciasSinResolver, OffsetDateTime recibidaEn) {
        return new Conformidad(true, incidenciasSinResolver, recibidaEn);
    }

    public boolean bloqueaEmision() {
        return !registrada || !incidenciasSinResolver.isEmpty();
    }
}
