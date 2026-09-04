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
 * Resultado del check-list de salida. Sostiene EJV-01 junto con {@code EjecucionDeViaje.iniciar}.
 *
 * <p>Clase inmutable y no {@code record} por la misma razon que {@link Evidencia}: posee una
 * coleccion, y Hibernate no puede rellenar los componentes finales de un record.
 */
@Embeddable
public class ResultadoDeCheckList {

    @Column(name = "checklist_aprobado", nullable = false)
    private boolean aprobado;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "checklist_observaciones",
        joinColumns = @JoinColumn(name = "checklist_id"))
    @Column(name = "observacion", length = 300, nullable = false)
    private List<String> observaciones = new ArrayList<>();

    @Column(name = "checklist_momento", nullable = false)
    private OffsetDateTime momento;

    /** Exigido por JPA. No usar: no valida nada. */
    protected ResultadoDeCheckList() {
    }

    public ResultadoDeCheckList(boolean aprobado, List<String> observaciones, OffsetDateTime momento) {
        if (momento == null) {
            throw new IllegalArgumentException("El momento del check-list es obligatorio");
        }
        List<String> copia = observaciones != null ? List.copyOf(observaciones) : List.of();
        if (!aprobado && (copia.isEmpty() || copia.stream().allMatch(String::isBlank))) {
            throw new IllegalArgumentException("Un check-list no aprobado exige observaciones no vacias");
        }
        this.aprobado = aprobado;
        this.observaciones.addAll(copia);
        this.momento = momento;
    }

    public static ResultadoDeCheckList aprobado(OffsetDateTime momento) {
        return new ResultadoDeCheckList(true, List.of(), momento);
    }

    public static ResultadoDeCheckList noAprobado(List<String> observaciones, OffsetDateTime momento) {
        return new ResultadoDeCheckList(false, observaciones, momento);
    }

    public boolean aprobado() {
        return aprobado;
    }

    public List<String> observaciones() {
        return List.copyOf(observaciones);
    }

    public OffsetDateTime momento() {
        return momento;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResultadoDeCheckList otro)) {
            return false;
        }
        return aprobado == otro.aprobado
            && Objects.equals(observaciones, otro.observaciones)
            && Objects.equals(momento, otro.momento);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aprobado, observaciones, momento);
    }
}
