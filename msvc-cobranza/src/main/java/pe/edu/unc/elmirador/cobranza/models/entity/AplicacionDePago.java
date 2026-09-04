package pe.edu.unc.elmirador.cobranza.models.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import pe.edu.unc.elmirador.cobranza.models.vo.Dinero;

/**
 * Entidad hija del agregado Pago.
 * Representa la aplicacion de una parte o la totalidad del monto de un pago a una cuenta por cobrar.
 */
@Entity
@Table(name = "aplicaciones")
public class AplicacionDePago {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "cuenta_por_cobrar_id", length = 40, nullable = false)
    private String cuentaPorCobrarId;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "monto", column = @Column(name = "importe_monto", precision = 15, scale = 2, nullable = false)),
        @AttributeOverride(name = "codigoMoneda", column = @Column(name = "importe_moneda", length = 3, nullable = false))
    })
    private Dinero importe;

    /** Exigido por JPA. No usar: no valida ninguna invariante. */
    protected AplicacionDePago() {
    }

    public AplicacionDePago(String id, String cuentaPorCobrarId, Dinero importe) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id de la aplicacion es obligatorio");
        }
        if (cuentaPorCobrarId == null || cuentaPorCobrarId.isBlank()) {
            throw new IllegalArgumentException("El id de la cuenta por cobrar es obligatorio");
        }
        if (importe == null) {
            throw new IllegalArgumentException("El importe de la aplicacion es obligatorio");
        }
        if (importe.esCero()) {
            throw new IllegalArgumentException("El importe de la aplicacion debe ser mayor a cero");
        }
        this.id = id.trim();
        this.cuentaPorCobrarId = cuentaPorCobrarId.trim();
        this.importe = importe;
    }

    public String id() {
        return id;
    }

    public String getId() {
        return id;
    }

    public String cuentaPorCobrarId() {
        return cuentaPorCobrarId;
    }

    public String getCuentaPorCobrarId() {
        return cuentaPorCobrarId;
    }

    public Dinero importe() {
        return importe;
    }

    public Dinero getImporte() {
        return importe;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AplicacionDePago that = (AplicacionDePago) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
