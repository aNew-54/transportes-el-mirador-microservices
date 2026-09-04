package pe.edu.unc.elmirador.ejecucion.models.entity;

import pe.edu.unc.elmirador.ejecucion.exceptions.GastoSinComprobanteException;
import pe.edu.unc.elmirador.ejecucion.models.vo.ConceptoDeGasto;
import pe.edu.unc.elmirador.ejecucion.models.vo.Comprobante;
import pe.edu.unc.elmirador.ejecucion.models.vo.Dinero;

public class GastoDeRuta {

    private final String id;
    private final ConceptoDeGasto concepto;
    private final Dinero importe;
    private final Comprobante comprobante;
    private final String descripcion;

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
