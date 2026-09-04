package pe.edu.unc.elmirador.unidades.models.entity;

import pe.edu.unc.elmirador.unidades.models.vo.Dinero;

public class TrabajoRealizado {

    private final String id;
    private final String descripcion;
    private final Dinero costoManoDeObra;
    private final String repuestoId;
    private final int cantidad;

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
}
