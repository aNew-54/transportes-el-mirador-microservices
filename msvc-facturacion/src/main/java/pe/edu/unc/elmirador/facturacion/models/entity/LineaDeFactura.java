package pe.edu.unc.elmirador.facturacion.models.entity;

import pe.edu.unc.elmirador.facturacion.models.vo.ConceptoFacturable;
import pe.edu.unc.elmirador.facturacion.models.vo.Dinero;

/**
 * Entidad hija del agregado Factura. Representa un item o concepto facturado.
 */
public class LineaDeFactura {

    private final String id;
    private final String ordenDeServicioId;
    private final ConceptoFacturable concepto;
    private final String descripcion;
    private final Dinero importe;

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
