package pe.edu.unc.elmirador.unidades.models.vo;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public record EstadoOperativo(
        @Enumerated(EnumType.STRING)
        SituacionOperativa situacion,
        String motivo) {

    public EstadoOperativo {
        if (situacion == null) {
            throw new IllegalArgumentException("La situacion operativa no puede ser nula");
        }
        if (situacion != SituacionOperativa.OPERATIVA) {
            if (motivo == null || motivo.trim().isEmpty()) {
                throw new IllegalArgumentException("Toda situacion distinta de OPERATIVA exige motivo no vacio");
            }
            motivo = motivo.trim();
        } else {
            motivo = null;
        }
    }

    public boolean esAsignable() {
        return situacion == SituacionOperativa.OPERATIVA;
    }

    public static EstadoOperativo operativa() {
        return new EstadoOperativo(SituacionOperativa.OPERATIVA, null);
    }

    public static EstadoOperativo enTaller(String motivo) {
        return new EstadoOperativo(SituacionOperativa.EN_TALLER, motivo);
    }

    public static EstadoOperativo inoperativa(String motivo) {
        return new EstadoOperativo(SituacionOperativa.INOPERATIVA, motivo);
    }
}
