package pe.edu.unc.elmirador.conductores.models.vo;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public record EstadoDeHabilitacion(
        @Enumerated(EnumType.STRING)
        SituacionDeHabilitacion situacion,
        String motivo
) {

    public EstadoDeHabilitacion {
        if (situacion == null) {
            throw new IllegalArgumentException("La situacion de habilitacion no puede ser nula");
        }
        if (situacion == SituacionDeHabilitacion.HABILITADO) {
            motivo = null;
        } else {
            if (motivo == null || motivo.isBlank()) {
                throw new IllegalArgumentException("El motivo es obligatorio cuando la situacion no es HABILITADO");
            }
            motivo = motivo.trim();
        }
    }

    public static EstadoDeHabilitacion habilitado() {
        return new EstadoDeHabilitacion(SituacionDeHabilitacion.HABILITADO, null);
    }

    public static EstadoDeHabilitacion suspendido(String motivo) {
        return new EstadoDeHabilitacion(SituacionDeHabilitacion.SUSPENDIDO, motivo);
    }

    public boolean estaHabilitado() {
        return situacion == SituacionDeHabilitacion.HABILITADO;
    }
}
