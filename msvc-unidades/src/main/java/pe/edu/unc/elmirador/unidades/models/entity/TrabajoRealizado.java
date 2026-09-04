package pe.edu.unc.elmirador.unidades.models.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import pe.edu.unc.elmirador.unidades.models.vo.Dinero;

@Entity
@Table(name = "trabajos_realizados")
public class TrabajoRealizado {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "descripcion", length = 300, nullable = false)
    private String descripcion;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "monto", column = @Column(name = "costo_mano_obra_monto", precision = 10, scale = 2, nullable = false)),
        @AttributeOverride(name = "codigoMoneda", column = @Column(name = "costo_mano_obra_moneda", length = 3, nullable = false))
    })
    private Dinero costoManoDeObra;

    @Column(name = "repuesto_id", length = 40)
    private String repuestoId;

    @Column(name = "cantidad", nullable = false)
    private int cantidad;

    /** Exigido por JPA. No usar: no valida ninguna invariante. */
    protected TrabajoRealizado() {
    }

    public TrabajoRealizado(String id, String descripcion, Dinero costoManoDeObra, String repuestoId, int cantidad) {
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripcion no puede estar vacia");
        }
        if (costoManoDeObra == null) {
            throw new IllegalArgumentException("El costo de mano de obra no puede ser nulo");
        }
        if (cantidad < 0) {
            throw new IllegalArgumentException("La cantidad de repuestos no puede ser negativa: " + cantidad);
        }
        this.id = id;
        this.descripcion = descripcion;
        this.costoManoDeObra = costoManoDeObra;
        this.repuestoId = repuestoId;
        this.cantidad = cantidad;
    }

    public TrabajoRealizado(String id, String descripcion, Dinero costoManoDeObra) {
        this(id, descripcion, costoManoDeObra, null, 0);
    }

    public String getId() {
        return id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Dinero getCostoManoDeObra() {
        return costoManoDeObra;
    }

    public String getRepuestoId() {
        return repuestoId;
    }

    public int getCantidad() {
        return cantidad;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrabajoRealizado that = (TrabajoRealizado) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
