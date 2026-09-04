package pe.edu.unc.elmirador.programacion.models.vo;

import jakarta.persistence.Embeddable;
import java.time.OffsetDateTime;

@Embeddable
public record VentanaDeTiempo(OffsetDateTime desde, OffsetDateTime hasta) {

    public VentanaDeTiempo {
        if (desde == null) {
            throw new IllegalArgumentException("La fecha-hora 'desde' es obligatoria");
        }
        if (hasta == null) {
            throw new IllegalArgumentException("La fecha-hora 'hasta' es obligatoria");
        }
        if (!hasta.isAfter(desde)) {
            throw new IllegalArgumentException(
                "La fecha-hora 'hasta' (" + hasta + ") debe ser posterior a 'desde' (" + desde + ")"
            );
        }
    }

    public boolean seSolapaCon(VentanaDeTiempo otra) {
        if (otra == null) {
            throw new IllegalArgumentException("La otra ventana de tiempo es obligatoria");
        }
        return this.desde.isBefore(otra.hasta) && otra.desde.isBefore(this.hasta);
    }
}
