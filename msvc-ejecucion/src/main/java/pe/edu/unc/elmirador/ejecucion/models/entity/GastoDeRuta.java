package pe.edu.unc.elmirador.ejecucion.models.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Embedded;

import pe.edu.unc.elmirador.ejecucion.exceptions.GastoSinComprobanteException;
import pe.edu.unc.elmirador.ejecucion.models.vo.ConceptoDeGasto;
import pe.edu.unc.elmirador.ejecucion.models.vo.Comprobante;
import pe.edu.unc.elmirador.ejecucion.models.vo.Dinero;

@Entity
@Table(name = "gastos_de_ruta")
public class GastoDeRuta {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "concepto", length = 20, nullable = false)
    private ConceptoDeGasto concepto;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "monto", column = @Column(name = "importe_monto", precision = 15, scale = 2, nullable = false)),
        @AttributeOverride(name = "codigoMoneda", column = @Column(name = "importe_moneda", length = 3, nullable = false))
    })
    private Dinero importe;

    /** LIQ-01: sin comprobante no hay gasto. Obligatorio tambien en la columna. */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "tipo", column = @Column(name = "comprobante_tipo", length = 30, nullable = false)),
        @AttributeOverride(name = "numero", column = @Column(name = "comprobante_numero", length = 40, nullable = false)),
        @AttributeOverride(name = "fecha", column = @Column(name = "comprobante_fecha", nullable = false))
    })
    private Comprobante comprobante;

    @Column(name = "descripcion", length = 300)
    private String descripcion;

    /** Exigido por JPA. No usar: no valida nada. */
    protected GastoDeRuta() {
    }

    public GastoDeRuta(
            String id,
            ConceptoDeGasto concepto,
            Dinero importe,
            Comprobante comprobante,
            String descripcion
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id del gasto es obligatorio");
        }
        if (concepto == null) {
            throw new IllegalArgumentException("El concepto de gasto es obligatorio");
        }
        if (importe == null) {
            throw new IllegalArgumentException("El importe del gasto es obligatorio");
        }
        if (comprobante == null) {
            throw new GastoSinComprobanteException("Todo gasto rendido debe contar con comprobante (LIQ-01)");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripcion del gasto es obligatoria");
        }
        this.id = id.trim();
        this.concepto = concepto;
        this.importe = importe;
        this.comprobante = comprobante;
        this.descripcion = descripcion.trim();
    }

    public String getId() {
        return id;
    }

    public ConceptoDeGasto getConcepto() {
        return concepto;
    }

    public Dinero getImporte() {
        return importe;
    }

    public Comprobante getComprobante() {
        return comprobante;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
