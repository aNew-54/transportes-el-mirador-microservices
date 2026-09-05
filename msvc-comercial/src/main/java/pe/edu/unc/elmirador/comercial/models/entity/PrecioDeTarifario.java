package pe.edu.unc.elmirador.comercial.models.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import pe.edu.unc.elmirador.comercial.models.vo.Dinero;
import pe.edu.unc.elmirador.comercial.models.vo.Ruta;
import pe.edu.unc.elmirador.comercial.models.vo.TipoDeUnidad;

/**
 * Entidad hija de Tarifario.
 * Representa el precio estandar fijado para una ruta y tipo de unidad.
 */
@Entity
@Table(name = "precios_de_tarifario")
public class PrecioDeTarifario {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "origen", column = @Column(name = "ruta_origen", length = 100, nullable = false)),
        @AttributeOverride(name = "destino", column = @Column(name = "ruta_destino", length = 100, nullable = false)),
        @AttributeOverride(name = "corredor", column = @Column(name = "ruta_corredor", length = 100, nullable = false))
    })
    private Ruta ruta;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_unidad", length = 20, nullable = false)
    private TipoDeUnidad tipoUnidad;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "monto", column = @Column(name = "precio_monto", precision = 15, scale = 2, nullable = false)),
        @AttributeOverride(name = "codigoMoneda", column = @Column(name = "precio_moneda", length = 3, nullable = false))
    })
    private Dinero precio;

    /** Exigido por JPA. No usar: no valida ninguna invariante. */
    protected PrecioDeTarifario() {
    }

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
