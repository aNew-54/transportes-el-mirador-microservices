package pe.edu.unc.elmirador.facturacion.models.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Huella de un {@code POST} de integracion ya atendido.
 *
 * <p>La regla 6 de los contratos exige que un reintento con la misma {@code Idempotency-Key} devuelva
 * el resultado original sin repetir el efecto. La clave la construye el consumidor con la forma que su
 * contrato fija; aqui se guarda tal cual y no se interpreta.
 *
 * <p>No es una entidad de dominio: no sostiene ninguna invariante y no pertenece a ningun agregado.
 * Es la memoria de la frontera HTTP.
 */
@Entity
@Table(name = "peticiones_idempotentes")
public class PeticionIdempotente {

    @Id
    @Column(name = "clave", length = 200, nullable = false)
    private String clave;

    @Column(name = "recurso_id", length = 40, nullable = false)
    private String recursoId;

    @Column(name = "registrada_en", nullable = false)
    private OffsetDateTime registradaEn;

    /** Exigido por JPA. */
    protected PeticionIdempotente() {
    }

    public PeticionIdempotente(String clave, String recursoId, OffsetDateTime registradaEn) {
        if (clave == null || clave.isBlank()) {
            throw new IllegalArgumentException("La clave de idempotencia no puede ser nula ni vacia");
        }
        if (recursoId == null || recursoId.isBlank()) {
            throw new IllegalArgumentException("El recursoId no puede ser nulo ni vacio");
        }
        if (registradaEn == null) {
            throw new IllegalArgumentException("La fecha de registro no puede ser nula");
        }
        this.clave = clave.trim();
        this.recursoId = recursoId.trim();
        this.registradaEn = registradaEn;
    }

    public String getClave() {
        return clave;
    }

    public String getRecursoId() {
        return recursoId;
    }

    public OffsetDateTime getRegistradaEn() {
        return registradaEn;
    }
}
