package pe.edu.unc.elmirador.ejecucion.models.vo;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;

@Embeddable
public record EsperaFacturable(OffsetDateTime inicio, OffsetDateTime fin, int tiempoLibreHoras) {

    public EsperaFacturable {
        if (inicio == null) {
            throw new IllegalArgumentException("La fecha de inicio es obligatoria");
        }
        if (fin == null) {
            throw new IllegalArgumentException("La fecha de fin es obligatoria");
        }
        if (!fin.isAfter(inicio)) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior a la fecha de inicio");
        }
        if (tiempoLibreHoras < 0) {
            throw new IllegalArgumentException("El tiempo libre no puede ser negativo: " + tiempoLibreHoras);
        }
    }

    public double tiempoRealHoras() {
        BigDecimal segundos = BigDecimal.valueOf(Duration.between(inicio, fin).toSeconds());
        return segundos.divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP).doubleValue();
    }

    public double excedente() {
        double real = tiempoRealHoras();
        if (real <= tiempoLibreHoras) {
            return 0.0;
        }
        return BigDecimal.valueOf(real - tiempoLibreHoras)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
