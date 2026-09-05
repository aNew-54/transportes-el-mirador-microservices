package pe.edu.unc.elmirador.comercial.models.vo;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Objeto de valor inmutable que representa la estructura tarifaria de un flete.
 * El calculo de subtotal y total es derivado (D8) y normativo:
 * primero los recargos sobre la base, y luego el descuento sobre el subtotal.
 *
 * <p><strong>Es clase inmutable y no {@code record}</strong>: un objeto de valor que posee una
 * coleccion no puede ser un record en Hibernate, porque al instanciarlo por reflexion la lista
 * quedaria vacia o nula en el constructor canonico.
 */
@Embeddable
public class Tarifa {

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "monto", column = @Column(name = "tarifa_base_monto", precision = 15, scale = 2)),
        @AttributeOverride(name = "codigoMoneda", column = @Column(name = "tarifa_base_moneda", length = 3))
    })
    private Dinero base;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tarifa_recargos", joinColumns = @JoinColumn(name = "tarifa_id"))
    private List<Recargo> recargos = new ArrayList<>();

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "porcentaje", column = @Column(name = "tarifa_descuento_porcentaje", precision = 5, scale = 2)),
        @AttributeOverride(name = "autorizadoPor", column = @Column(name = "tarifa_descuento_autorizado_por", length = 100))
    })
    private Descuento descuento;

    /** Exigido por JPA. No usar: no valida nada. */
    protected Tarifa() {
    }

    public Tarifa(Dinero base, List<Recargo> recargos, Descuento descuento) {
        if (base == null) {
            throw new IllegalArgumentException("La tarifa base es obligatoria");
        }
        if (recargos == null) {
            throw new IllegalArgumentException("La lista de recargos no puede ser nula");
        }
        this.base = base;
        this.recargos.addAll(recargos);
        this.descuento = descuento;
    }

    public Tarifa(Dinero base) {
        this(base, List.of(), null);
    }

    public Tarifa(Dinero base, List<Recargo> recargos) {
        this(base, recargos, null);
    }

    public Dinero base() {
        return base;
    }

    public List<Recargo> recargos() {
        return List.copyOf(recargos);
    }

    public Descuento descuento() {
        return descuento;
    }

    public Dinero subtotal() {
        if (base == null) {
            return null;
        }
        BigDecimal sumaRecargos = BigDecimal.ZERO;
        for (Recargo recargo : recargos) {
            BigDecimal montoRecargo = base.monto()
                .multiply(recargo.porcentaje())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            sumaRecargos = sumaRecargos.add(montoRecargo);
        }
        return new Dinero(base.monto().add(sumaRecargos), base.codigoMoneda());
    }

    public Dinero total() {
        if (base == null) {
            return null;
        }
        Dinero sub = subtotal();
        if (descuento == null) {
            return sub;
        }
        BigDecimal montoDescuento = sub.monto()
            .multiply(descuento.porcentaje())
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        return new Dinero(sub.monto().subtract(montoDescuento), base.codigoMoneda());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Tarifa otra)) {
            return false;
        }
        return Objects.equals(base, otra.base)
            && Objects.equals(recargos, otra.recargos)
            && Objects.equals(descuento, otra.descuento);
    }

    @Override
    public int hashCode() {
        return Objects.hash(base, recargos, descuento);
    }
}
