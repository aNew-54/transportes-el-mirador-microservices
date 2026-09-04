package pe.edu.unc.elmirador.unidades.models.entity;

import pe.edu.unc.elmirador.unidades.exceptions.ExistenciasNegativasException;
import pe.edu.unc.elmirador.unidades.models.vo.Dinero;

public class Repuesto {

    private final String id;
    private final String codigo;
    private final String descripcion;
    private int existencias;
    private final int stockMinimo;
    private final Dinero costoUnitario;

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
}
