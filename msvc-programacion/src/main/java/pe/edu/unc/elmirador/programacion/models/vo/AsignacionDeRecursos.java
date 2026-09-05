package pe.edu.unc.elmirador.programacion.models.vo;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Embeddable
public class AsignacionDeRecursos {

    @Column(name = "unidad_id", length = 40)
    private String unidadId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "viaje_conductores",
            joinColumns = @JoinColumn(name = "viaje_id")
    )
    @Column(name = "conductor_id", length = 40, nullable = false)
    private List<String> conductorIds = new ArrayList<>();

    @Column(name = "con_relevo")
    private Boolean conRelevo;

    /** Exigido por JPA. No usar: no valida nada. */
    protected AsignacionDeRecursos() {
    }

    public AsignacionDeRecursos(String unidadId, List<String> conductorIds, boolean conRelevo) {
        this.unidadId = (unidadId != null && !unidadId.isBlank()) ? unidadId.trim() : null;
        if (conductorIds != null) {
            for (String cid : conductorIds) {
                if (cid == null || cid.isBlank()) {
                    throw new IllegalArgumentException("El identificador del conductor no puede ser nulo ni vacio");
                }
            }
            this.conductorIds.addAll(conductorIds);
        }

        if (this.conductorIds.size() > 2) {
            throw new IllegalArgumentException("No se permiten tres o mas conductores");
        }
        if (this.conductorIds.size() == 2 && !conRelevo) {
            throw new IllegalArgumentException("Un segundo conductor solo se permite en viajes con relevo");
        }
        this.conRelevo = conRelevo;
    }

    public boolean esCompleta() {
        return unidadId != null && !unidadId.isBlank() && !conductorIds.isEmpty();
    }

    public String unidadId() {
        return unidadId;
    }

    public List<String> conductorIds() {
        return List.copyOf(conductorIds);
    }

    public boolean conRelevo() {
        return Boolean.TRUE.equals(conRelevo);
    }

    public static AsignacionDeRecursos de(String unidadId, String conductorId) {
        if (conductorId == null || conductorId.isBlank()) {
            throw new IllegalArgumentException("El identificador del conductor es obligatorio");
        }
        return new AsignacionDeRecursos(unidadId, List.of(conductorId), false);
    }

    public static AsignacionDeRecursos conRelevo(String unidadId, String conductorPrincipalId, String conductorRelevoId) {
        if (conductorPrincipalId == null || conductorPrincipalId.isBlank()) {
            throw new IllegalArgumentException("El conductor principal es obligatorio");
        }
        if (conductorRelevoId == null || conductorRelevoId.isBlank()) {
            throw new IllegalArgumentException("El conductor de relevo es obligatorio");
        }
        return new AsignacionDeRecursos(unidadId, List.of(conductorPrincipalId, conductorRelevoId), true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AsignacionDeRecursos otra)) return false;
        return Objects.equals(unidadId, otra.unidadId)
                && Objects.equals(conductorIds, otra.conductorIds)
                && Objects.equals(conRelevo(), otra.conRelevo());
    }

    @Override
    public int hashCode() {
        return Objects.hash(unidadId, conductorIds, conRelevo());
    }

    @Override
    public String toString() {
        return "AsignacionDeRecursos[unidadId=" + unidadId + ", conductorIds=" + conductorIds + ", conRelevo=" + conRelevo() + "]";
    }
}
