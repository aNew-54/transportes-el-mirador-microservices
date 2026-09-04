package pe.edu.unc.elmirador.facturacion.models.vo;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Conformidad de entrega recibida desde Ejecucion por el contrato 8.
 *
 * <p><strong>Es clase inmutable y no {@code record}, y la razon es del mapeo:</strong> un objeto de
 * valor que posee una coleccion no puede ser un record. Hibernate construye el record entero por su
 * constructor canonico y solo despues rellena la coleccion, asi que al leer le pasaria {@code null}
 * y el constructor compacto lanzaria. Y aunque tolerase el nulo, los componentes de un record son
 * {@code final}: la lista se quedaria vacia para siempre, en silencio, que es peor que fallar.
 *
 * <p>Hacia fuera se comporta igual que un record: sin identidad, sin setters, con accesores del
 * mismo nombre y la lista expuesta como copia inmutable.
 */
@Embeddable
public class Conformidad {

    @Column(name = "conformidad_registrada", nullable = false)
    private boolean registrada;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "factura_incidencias",
        joinColumns = @JoinColumn(name = "factura_id"))
    @Column(name = "incidencia", length = 200, nullable = false)
    private List<String> incidenciasSinResolver = new ArrayList<>();

    @Column(name = "conformidad_recibida_en")
    private OffsetDateTime recibidaEn;

    /** Exigido por JPA. No usar: no valida nada. */
    protected Conformidad() {
    }

    public Conformidad(boolean registrada, List<String> incidenciasSinResolver, OffsetDateTime recibidaEn) {
        if (incidenciasSinResolver == null) {
            throw new IllegalArgumentException(
                "La lista de incidencias sin resolver es obligatoria y no puede ser nula");
        }
        if (registrada && recibidaEn == null) {
            throw new IllegalArgumentException(
                "El instante de recepcion es obligatorio cuando la conformidad esta registrada");
        }
        this.registrada = registrada;
        this.incidenciasSinResolver.addAll(incidenciasSinResolver);
        this.recibidaEn = recibidaEn;
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

    public boolean registrada() {
        return registrada;
    }

    public List<String> incidenciasSinResolver() {
        return List.copyOf(incidenciasSinResolver);
    }

    public OffsetDateTime recibidaEn() {
        return recibidaEn;
    }

    /** FAC-05: una incidencia sin resolver bloquea la emision, y no estar registrada tambien. */
    public boolean bloqueaEmision() {
        return !registrada || !incidenciasSinResolver.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Conformidad otra)) {
            return false;
        }
        return registrada == otra.registrada
            && Objects.equals(recibidaEn, otra.recibidaEn)
            && Objects.equals(incidenciasSinResolver, otra.incidenciasSinResolver);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registrada, incidenciasSinResolver, recibidaEn);
    }
}
