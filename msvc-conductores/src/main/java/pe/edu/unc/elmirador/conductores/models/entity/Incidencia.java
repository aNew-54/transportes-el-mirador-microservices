package pe.edu.unc.elmirador.conductores.models.entity;

import java.time.OffsetDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Incidencia ocurrida en ruta y atribuida —o no— al conductor. Entidad hija del agregado
 * {@code Conductor}: llega por el contrato 6, que Ejecucion empuja al terminar el viaje.
 *
 * <p>{@code tipo} se guarda como texto y no como enumerado a proposito: el contrato 6 no enumera sus
 * valores, sólo muestra {@code DOCUMENTARIA} de ejemplo. Inventar aqui la lista completa seria
 * codificar una regla que el diseno no ha fijado. Cuando el catalogo exista, se convierte.
 *
 * <p>{@code atribuible} lo decide Ejecucion, que es quien vio lo que paso. Conductores lo registra;
 * no lo reevalua.
 *
 * <p>{@code registradaEn} es el momento en que Conductores recibe el reporte, no el momento en que la
 * incidencia ocurrio: el contrato 6 no envia esa fecha. El nombre lo dice para que nadie la lea como
 * lo que no es.
 */
@Entity
@Table(name = "incidencias")
public class Incidencia {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "viaje_id", length = 40, nullable = false)
    private String viajeId;

    @Column(name = "tipo", length = 40, nullable = false)
    private String tipo;

    @Column(name = "descripcion", length = 500, nullable = false)
    private String descripcion;

    @Column(name = "atribuible", nullable = false)
    private boolean atribuible;

    @Column(name = "registrada_en", nullable = false)
    private OffsetDateTime registradaEn;

    /** Exigido por JPA. No usar: no valida ninguna invariante. */
    protected Incidencia() {
    }

    public Incidencia(
            String id,
            String viajeId,
            String tipo,
            String descripcion,
            boolean atribuible,
            OffsetDateTime registradaEn
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id de la incidencia no puede ser nulo ni vacio");
        }
        if (viajeId == null || viajeId.isBlank()) {
            throw new IllegalArgumentException("El viajeId no puede ser nulo ni vacio");
        }
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("El tipo de incidencia no puede ser nulo ni vacio");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripcion no puede ser nula ni vacia");
        }
        if (registradaEn == null) {
            throw new IllegalArgumentException("El momento de registro no puede ser nulo");
        }

        this.id = id.trim();
        this.viajeId = viajeId.trim();
        this.tipo = tipo.trim();
        this.descripcion = descripcion.trim();
        this.atribuible = atribuible;
        this.registradaEn = registradaEn;
    }

    public String getId() {
        return id;
    }

    public String getViajeId() {
        return viajeId;
    }

    public String getTipo() {
        return tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean esAtribuible() {
        return atribuible;
    }

    public OffsetDateTime getOcurridaEn() {
        return registradaEn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Incidencia otra = (Incidencia) o;
        return Objects.equals(id, otra.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
