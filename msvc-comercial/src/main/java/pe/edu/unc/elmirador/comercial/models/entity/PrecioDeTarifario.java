package pe.edu.unc.elmirador.comercial.models.entity;

import pe.edu.unc.elmirador.comercial.models.vo.Dinero;
import pe.edu.unc.elmirador.comercial.models.vo.Ruta;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeUnidad;

/**
 * Entidad hija de Tarifario.
 * Representa el precio estandar fijado para una ruta y tipo de unidad.
 */
public class PrecioDeTarifario {

    private final String id;
    private final Ruta ruta;
    private final TipoDeUnidad tipoUnidad;
    private final Dinero precio;

    public PrecioDeTarifario(String id, Ruta ruta, TipoDeUnidad tipoUnidad, Dinero precio) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id del precio de tarifario es obligatorio");
        }
        if (ruta == null) {
            throw new IllegalArgumentException("La ruta es obligatoria");
        }
        if (tipoUnidad == null) {
            throw new IllegalArgumentException("El tipo de unidad es obligatorio");
        }
        if (precio == null) {
            throw new IllegalArgumentException("El precio es obligatorio");
        }
        this.id = id.trim();
        this.ruta = ruta;
        this.tipoUnidad = tipoUnidad;
        this.precio = precio;
    }

    public String id() {
        return id;
    }

    public Ruta ruta() {
        return ruta;
    }

    public TipoDeUnidad tipoUnidad() {
        return tipoUnidad;
    }

    public Dinero precio() {
        return precio;
    }
}
