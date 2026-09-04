package pe.edu.unc.elmirador.ejecucion.models.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import pe.edu.unc.elmirador.ejecucion.exceptions.DominioEjecucionException;
import pe.edu.unc.elmirador.ejecucion.models.vo.EsperaFacturable;
import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoDeParada;

@Entity
@Table(name = "paradas")
public class Parada {

    /**
     * Clave sustituta, exigida por JPA. La identidad de negocio de una parada es su secuencia
     * dentro de la ejecucion, que no es unica por si sola en la tabla: el dominio no la ve.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "secuencia", nullable = false)
    private int secuencia;

    @Column(name = "orden_de_servicio_id", length = 40, nullable = false)
    private String ordenDeServicioId;

    @Column(name = "direccion", length = 300)
    private String direccion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoDeParada estado;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "conformidad_id")
    private ConformidadDeEntrega conformidad;

    // Nula mientras no haya espera. Sus componentes se renombran para no chocar con nada mas.
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "inicio", column = @Column(name = "espera_inicio")),
        @AttributeOverride(name = "fin", column = @Column(name = "espera_fin")),
        @AttributeOverride(name = "tiempoLibreHoras", column = @Column(name = "espera_tiempo_libre"))
    })
    private EsperaFacturable esperaFacturable;

    /** Exigido por JPA. No usar: no valida nada. */
    protected Parada() {
    }

    public Parada(int secuencia, String ordenDeServicioId, String direccion) {
        if (secuencia <= 0) {
            throw new IllegalArgumentException("La secuencia de parada debe ser positiva: " + secuencia);
        }
        if (ordenDeServicioId == null || ordenDeServicioId.isBlank()) {
            throw new IllegalArgumentException("La orden de servicio es obligatoria");
        }
        if (direccion == null || direccion.isBlank()) {
            throw new IllegalArgumentException("La direccion es obligatoria");
        }
        this.secuencia = secuencia;
        this.ordenDeServicioId = ordenDeServicioId.trim();
        this.direccion = direccion.trim();
        this.estado = EstadoDeParada.PENDIENTE;
        this.conformidad = null;
        this.esperaFacturable = null;
    }

    public int getSecuencia() {
        return secuencia;
    }

    public String getOrdenDeServicioId() {
        return ordenDeServicioId;
    }

    public String getDireccion() {
        return direccion;
    }

    public EstadoDeParada getEstado() {
        return estado;
    }

    public ConformidadDeEntrega getConformidad() {
        return conformidad;
    }

    public EsperaFacturable getEsperaFacturable() {
        return esperaFacturable;
    }

    public void registrarConformidad(ConformidadDeEntrega conformidad) {
        if (conformidad == null) {
            throw new IllegalArgumentException("La conformidad es obligatoria");
        }
        if (this.conformidad != null || this.estado == EstadoDeParada.ATENDIDA) {
            throw new DominioEjecucionException("La parada ya cuenta con una conformidad registrada (EJV-02)");
        }
        if (!this.ordenDeServicioId.equals(conformidad.getOrdenDeServicioId())) {
            throw new DominioEjecucionException(
                "La orden de servicio de la conformidad (" + conformidad.getOrdenDeServicioId()
                    + ") no coincide con la parada (" + this.ordenDeServicioId + ") (EJV-02)"
            );
        }
        this.conformidad = conformidad;
        this.estado = EstadoDeParada.ATENDIDA;
    }

    public void registrarEsperaFacturable(EsperaFacturable esperaFacturable) {
        if (esperaFacturable == null) {
            throw new IllegalArgumentException("La espera facturable es obligatoria");
        }
        this.esperaFacturable = esperaFacturable;
    }

    public void llegarASitio() {
        if (this.estado == EstadoDeParada.ATENDIDA) {
            throw new DominioEjecucionException("La parada ya fue atendida");
        }
        this.estado = EstadoDeParada.EN_SITIO;
    }

    public void reabrir() {
        if (this.estado != EstadoDeParada.ATENDIDA) {
            throw new DominioEjecucionException("Solo se pueden reabrir paradas que esten atendidas");
        }
        this.estado = EstadoDeParada.EN_SITIO;
    }

    public boolean tieneConformidadFirmada() {
        return conformidad != null && conformidad.estaFirmada();
    }
}
