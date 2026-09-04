package pe.edu.unc.elmirador.unidades.models.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import pe.edu.unc.elmirador.unidades.exceptions.ExistenciasNegativasException;
import pe.edu.unc.elmirador.unidades.models.vo.Dinero;

@Entity
@Table(name = "repuestos")
public class Repuesto {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "codigo", length = 50, nullable = false)
    private String codigo;

    @Column(name = "descripcion", length = 300, nullable = false)
    private String descripcion;

    @Column(name = "existencias", nullable = false)
    private int existencias;

    @Column(name = "stock_minimo", nullable = false)
    private int stockMinimo;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "monto", column = @Column(name = "costo_unitario_monto", precision = 10, scale = 2)),
        @AttributeOverride(name = "codigoMoneda", column = @Column(name = "costo_unitario_moneda", length = 3))
    })
    private Dinero costoUnitario;

    /** Exigido por JPA. No usar: no valida ninguna invariante. */
    protected Repuesto() {
    }

    public Repuesto(
            String id,
            String codigo,
            String descripcion,
            int existencias,
            int stockMinimo,
            Dinero costoUnitario) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id del repuesto no puede estar vacio");
        }
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El codigo del repuesto no puede estar vacio");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripcion del repuesto no puede estar vacia");
        }
        if (existencias < 0) {
            throw new ExistenciasNegativasException(
                    "Las existencias iniciales no pueden ser negativas (REP-01): " + existencias);
        }
        if (stockMinimo < 0) {
            throw new IllegalArgumentException("El stock minimo no puede ser negativo: " + stockMinimo);
        }
        this.id = id;
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.existencias = existencias;
        this.stockMinimo = stockMinimo;
        this.costoUnitario = costoUnitario;
    }

    public void ajustarInventario(int cantidad) {
        int nuevasExistencias = this.existencias + cantidad;
        if (nuevasExistencias < 0) {
            throw new ExistenciasNegativasException(
                    "El ajuste de inventario dejaria existencias negativas (REP-01): actual="
                            + this.existencias + ", ajuste=" + cantidad + ", resultado=" + nuevasExistencias);
        }
        this.existencias = nuevasExistencias;
    }

    public boolean requiereReposicion() {
        return this.existencias <= this.stockMinimo;
    }

    public String getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getExistencias() {
        return existencias;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public Dinero getCostoUnitario() {
        return costoUnitario;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Repuesto repuesto = (Repuesto) o;
        return Objects.equals(id, repuesto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
