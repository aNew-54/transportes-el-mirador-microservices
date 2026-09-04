package pe.edu.unc.elmirador.programacion.models.vo;

import jakarta.persistence.Embeddable;
import java.util.List;

@Embeddable
public record AsignacionDeRecursos(String unidadId, List<String> conductorIds, boolean conRelevo) {

    public AsignacionDeRecursos {
        unidadId = (unidadId != null && !unidadId.isBlank()) ? unidadId.trim() : null;
        if (conductorIds != null) {
            for (String cid : conductorIds) {
                if (cid == null || cid.isBlank()) {
                    throw new IllegalArgumentException("El identificador del conductor no puede ser nulo ni vacio");
                }
            }
            conductorIds = List.copyOf(conductorIds);
        } else {
            conductorIds = List.of();
        }

        if (conductorIds.size() > 2) {
            throw new IllegalArgumentException("No se permiten tres o mas conductores");
        }
        if (conductorIds.size() == 2 && !conRelevo) {
            throw new IllegalArgumentException("Un segundo conductor solo se permite en viajes con relevo");
        }
    }

    public boolean esCompleta() {
        return unidadId != null && !unidadId.isBlank() && !conductorIds.isEmpty();
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
}
