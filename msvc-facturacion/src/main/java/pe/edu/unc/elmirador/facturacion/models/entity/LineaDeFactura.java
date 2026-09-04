package pe.edu.unc.elmirador.facturacion.models.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import pe.edu.unc.elmirador.facturacion.models.vo.ConceptoFacturable;
import pe.edu.unc.elmirador.facturacion.models.vo.Dinero;

/**
 * Entidad hija del agregado Factura. Representa un item o concepto facturado.
 */
@Entity
@Table(name = "lineas_de_factura")
public class LineaDeFactura {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "orden_de_servicio_id", length = 40, nullable = false)
    private String ordenDeServicioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "concepto", length = 20, nullable = false)
    private ConceptoFacturable concepto;

    @Column(name = "descripcion", length = 300, nullable = false)
    private String descripcion;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "monto", column = @Column(name = "importe_monto", precision = 15, scale = 2, nullable = false)),
        @AttributeOverride(name = "codigoMoneda", column = @Column(name = "importe_moneda", length = 3, nullable = false))
    })
    private Dinero importe;

    /** Exigido por JPA. No usar: no valida ninguna invariante. */
    protected LineaDeFactura() {
    }

    public LineaDeFactura(
        String id,
        String ordenDeServicioId,
        ConceptoFacturable concepto,
        String descripcion,
        Dinero importe
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id de la linea de factura es obligatorio");
        }
        if (ordenDeServicioId == null || ordenDeServicioId.isBlank()) {
            throw new IllegalArgumentException(
                "La orden de servicio de la linea es obligatoria: sin ella FAC-02 no se puede comprobar");
        }
        if (concepto == null) {
            throw new IllegalArgumentException("El concepto facturable es obligatorio");
        }
        if (importe == null) {
            throw new IllegalArgumentException("El importe de la linea es obligatorio");
        }
        this.id = id.trim();
        this.ordenDeServicioId = ordenDeServicioId.trim();
        this.concepto = concepto;
        this.descripcion = descripcion != null ? descripcion.trim() : "";
        this.importe = importe;
    }

    public String id() {
        return id;
    }

    public String ordenDeServicioId() {
        return ordenDeServicioId;
    }

    public ConceptoFacturable concepto() {
        return concepto;
    }

    public String descripcion() {
        return descripcion;
    }

    public Dinero importe() {
        return importe;
    }
}
