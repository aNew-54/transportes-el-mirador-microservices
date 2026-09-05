package pe.edu.unc.elmirador.comercial.models.entity;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Espera facturable ocurrida en un punto del viaje. Llega por el contrato 7.
 *
 * <p>El {@code excedenteHoras} lo calcula Ejecución con {@code EsperaFacturable.excedente()};
 * Comercial lo consume y no lo recalcula. Reinterpretar aquí un dato del que Ejecución es dueña
 * produciría dos verdades sobre el mismo hecho.
 *
 * <p>Sin esta entidad el contrato 7 no tenía dónde dejar lo que reporta: la primera versión del slice
 * guardaba la clave de idempotencia y descartaba el dato, de modo que la protección contra
 * duplicados protegía un efecto que no existía.
 */
@Entity
@Table(name = "esperas_registradas")
public class EsperaRegistrada {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "viaje_id", length = 40, nullable = false)
    private String viajeId;

    @Column(name = "punto", length = 20, nullable = false)
    private String punto;

    @Column(name = "tiempo_libre_horas", precision = 6, scale = 2, nullable = false)
    private BigDecimal tiempoLibreHoras;

    @Column(name = "tiempo_real_horas", precision = 6, scale = 2, nullable = false)
    private BigDecimal tiempoRealHoras;

    @Column(name = "excedente_horas", precision = 6, scale = 2, nullable = false)
    private BigDecimal excedenteHoras;

    /** Exigido por JPA. No usar: no valida ninguna invariante. */
    protected EsperaRegistrada() {
    }

    public EsperaRegistrada(
            String id,
            String viajeId,
            String punto,
            BigDecimal tiempoLibreHoras,
            BigDecimal tiempoRealHoras,
            BigDecimal excedenteHoras
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id de la espera no puede ser nulo ni vacio");
        }
        if (viajeId == null || viajeId.isBlank()) {
            throw new IllegalArgumentException("El viajeId no puede ser nulo ni vacio");
        }
        if (punto == null || punto.isBlank()) {
            throw new IllegalArgumentException("El punto de la espera no puede ser nulo ni vacio");
        }
        if (tiempoLibreHoras == null || tiempoLibreHoras.signum() < 0) {
            throw new IllegalArgumentException("El tiempo libre no puede ser nulo ni negativo");
        }
        if (tiempoRealHoras == null || tiempoRealHoras.signum() < 0) {
            throw new IllegalArgumentException("El tiempo real no puede ser nulo ni negativo");
        }
        if (excedenteHoras == null || excedenteHoras.signum() < 0) {
            throw new IllegalArgumentException("El excedente no puede ser nulo ni negativo");
        }

        this.id = id.trim();
        this.viajeId = viajeId.trim();
        this.punto = punto.trim().toUpperCase();
        this.tiempoLibreHoras = tiempoLibreHoras;
        this.tiempoRealHoras = tiempoRealHoras;
        this.excedenteHoras = excedenteHoras;
    }

    public String getId() {
        return id;
    }

    public String getViajeId() {
        return viajeId;
    }

    public String getPunto() {
        return punto;
    }

    public BigDecimal getTiempoLibreHoras() {
        return tiempoLibreHoras;
    }

    public BigDecimal getTiempoRealHoras() {
        return tiempoRealHoras;
    }

    public BigDecimal getExcedenteHoras() {
        return excedenteHoras;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EsperaRegistrada otra = (EsperaRegistrada) o;
        return Objects.equals(id, otra.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
