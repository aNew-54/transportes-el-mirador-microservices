package pe.edu.unc.elmirador.comercial.models.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import pe.edu.unc.elmirador.comercial.models.vo.CondicionDePago;
import pe.edu.unc.elmirador.comercial.models.vo.EstadoCrediticio;
import pe.edu.unc.elmirador.comercial.models.vo.RazonSocial;
import pe.edu.unc.elmirador.comercial.models.vo.Ruc;

/**
 * Raiz del agregado Cliente.
 * Sostiene la invariante CLI-01.
 */
@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Embedded
    @AttributeOverride(name = "valor", column = @Column(name = "ruc", length = 11, nullable = false))
    private Ruc ruc;

    @Embedded
    @AttributeOverride(name = "valor", column = @Column(name = "razon_social", length = 200, nullable = false))
    private RazonSocial razonSocial;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "modalidad", column = @Column(name = "condicion_habitual_modalidad", length = 10, nullable = false)),
        @AttributeOverride(name = "plazoEnDias", column = @Column(name = "condicion_habitual_plazo_dias", nullable = false))
    })
    private CondicionDePago condicionHabitual;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "situacion", column = @Column(name = "estado_crediticio_situacion", length = 20, nullable = false)),
        @AttributeOverride(name = "fechaDeCambio", column = @Column(name = "estado_crediticio_fecha_cambio", nullable = false))
    })
    private EstadoCrediticio estadoCrediticio;

    /** Exigido por JPA. No usar: no valida ninguna invariante. */
    protected Cliente() {
    }

    public Cliente(
        String id,
        Ruc ruc,
        RazonSocial razonSocial,
        CondicionDePago condicionHabitual,
        EstadoCrediticio estadoCrediticio
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id del cliente es obligatorio");
        }
        if (ruc == null) {
            throw new IllegalArgumentException("El RUC es obligatorio");
        }
        if (razonSocial == null) {
            throw new IllegalArgumentException("La razon social es obligatoria");
        }
        if (condicionHabitual == null) {
            throw new IllegalArgumentException("La condicion de pago habitual es obligatoria");
        }
        if (estadoCrediticio == null) {
            throw new IllegalArgumentException("El estado crediticio es obligatorio");
        }
        this.id = id.trim();
        this.ruc = ruc;
        this.razonSocial = razonSocial;
        this.condicionHabitual = condicionHabitual;
        this.estadoCrediticio = estadoCrediticio;
    }

    public String id() {
        return id;
    }

    public Ruc ruc() {
        return ruc;
    }

    public RazonSocial razonSocial() {
        return razonSocial;
    }

    public CondicionDePago condicionHabitual() {
        return condicionHabitual;
    }

    public EstadoCrediticio estadoCrediticio() {
        return estadoCrediticio;
    }

    /**
     * Invariante CLI-01 (primera mitad):
     * Un cliente con credito suspendido no puede contratar a credito.
     */
    public boolean puedeContratarACredito() {
        return this.estadoCrediticio.permiteCredito();
    }

    /**
     * Invariante CLI-01 (segunda mitad):
     * Un cliente suspendido si puede contratar al contado. Suspender no lo deja fuera del negocio.
     */
    public boolean puedeContratarAlContado() {
        return true;
    }

    /**
     * Sustituye la copia local del estado crediticio proveniente de Cobranza (Contrato 11).
     * Rechaza lecturas con fecha de cambio anterior a la vigente.
     */
    public void refrescarEstadoCrediticio(EstadoCrediticio nuevoEstado) {
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("El nuevo estado crediticio es obligatorio");
        }
        if (nuevoEstado.fechaDeCambio().isBefore(this.estadoCrediticio.fechaDeCambio())) {
            throw new IllegalArgumentException(
                "No se puede registrar un estado crediticio con fecha (" + nuevoEstado.fechaDeCambio()
                    + ") anterior a la vigente (" + this.estadoCrediticio.fechaDeCambio() + ")"
            );
        }
        this.estadoCrediticio = nuevoEstado;
    }

    /**
     * Refresca la copia local con una lectura del contrato 11, y no hace nada si la lectura es mas
     * antigua que la guardada.
     *
     * <p>Se diferencia de {@link #refrescarEstadoCrediticio} en que no protesta. Aquel protege contra
     * un dato empujado fuera de orden, donde una lectura vieja pisando a una nueva es un defecto que
     * hay que ver. Este cubre la consulta sincrona a la fuente de verdad, donde una fecha rara es un
     * reloj desajustado y no una razon para tumbar la creacion de una orden. La decision de ORD-02 se
     * toma con lo que Cobranza acaba de responder, no con esta copia.
     */
    public void refrescarSiEsMasReciente(EstadoCrediticio lectura) {
        if (lectura == null) {
            throw new IllegalArgumentException("La lectura del estado crediticio es obligatoria");
        }
        if (lectura.fechaDeCambio().isBefore(this.estadoCrediticio.fechaDeCambio())) {
            return;
        }
        this.estadoCrediticio = lectura;
    }

    public void cambiarCondicionHabitual(CondicionDePago nuevaCondicion) {
        if (nuevaCondicion == null) {
            throw new IllegalArgumentException("La nueva condicion habitual es obligatoria");
        }
        this.condicionHabitual = nuevaCondicion;
    }
}
