package pe.edu.unc.elmirador.ejecucion.models.vo;

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
 * Evidencia de una incidencia: fotografias, descripcion y momento.
 *
 * <p><strong>Clase inmutable y no {@code record}</strong>, y la razon es del mapeo: Hibernate
 * construye un record entero por su constructor canonico y solo despues rellena las colecciones,
 * asi que al leer le pasaria {@code null} a las fotografias. Y como los componentes de un record
 * son {@code final}, la lista jamas se rellenaria: quedaria vacia en silencio.
 */
@Embeddable
public class Evidencia {

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "incidencia_fotografias",
        joinColumns = @JoinColumn(name = "incidencia_id"))
    @Column(name = "fotografia", length = 500, nullable = false)
    private List<String> fotografias = new ArrayList<>();

    @Column(name = "evidencia_descripcion", length = 500)
    private String descripcion;

    @Column(name = "evidencia_momento")
    private OffsetDateTime momento;

    /** Exigido por JPA. No usar: no valida nada. */
    protected Evidencia() {
    }

    public Evidencia(List<String> fotografias, String descripcion, OffsetDateTime momento) {
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
        this.fotografias.addAll(fotografias);
        this.descripcion = descripcion.trim();
        this.momento = momento;
    }

    public List<String> fotografias() {
        return List.copyOf(fotografias);
    }

    public String descripcion() {
        return descripcion;
    }

    public OffsetDateTime momento() {
        return momento;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Evidencia otra)) {
            return false;
        }
        return Objects.equals(fotografias, otra.fotografias)
            && Objects.equals(descripcion, otra.descripcion)
            && Objects.equals(momento, otra.momento);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fotografias, descripcion, momento);
    }
}
