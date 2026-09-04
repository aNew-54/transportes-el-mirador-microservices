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

    private static final BigDecimal SEGUNDOS_POR_HORA = BigDecimal.valueOf(3600);

    public BigDecimal tiempoRealHoras() {
        return BigDecimal.valueOf(Duration.between(inicio, fin).toSeconds())
                .divide(SEGUNDOS_POR_HORA, 2, RoundingMode.HALF_UP);
    }

    /**
     * Horas que exceden el tiempo libre pactado, o cero.
     *
     * <p>Devuelve {@link BigDecimal} y no {@code double} a proposito: este valor viaja en los
     * contratos 7 y 8 y alli se multiplica por una tarifa horaria, asi que es un importe en
     * potencia. En coma flotante binaria, 0.1 hora facturada mil veces no suma 100.
     */
    public BigDecimal excedente() {
        BigDecimal real = tiempoRealHoras();
        BigDecimal libre = BigDecimal.valueOf(tiempoLibreHoras).setScale(2);
        if (real.compareTo(libre) <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return real.subtract(libre);
    }
}
