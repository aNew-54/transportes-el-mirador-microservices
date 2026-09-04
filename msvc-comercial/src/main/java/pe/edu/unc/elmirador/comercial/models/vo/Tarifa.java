package pe.edu.unc.elmirador.comercial.models.vo;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Objeto de valor inmutable que representa la estructura tarifaria de un flete.
 * El calculo de subtotal y total es derivado (D8) y normativo:
 * primero los recargos sobre la base, y luego el descuento sobre el subtotal.
 */
@Embeddable
public record Tarifa(Dinero base, List<Recargo> recargos, Descuento descuento) {

    public Tarifa {
        if (base == null) {
            throw new IllegalArgumentException("La tarifa base es obligatoria");
        }
        if (recargos == null) {
            throw new IllegalArgumentException("La lista de recargos no puede ser nula");
        }
        recargos = List.copyOf(recargos);
    }

    public Tarifa(Dinero base) {
        this(base, List.of(), null);
    }

    public Tarifa(Dinero base, List<Recargo> recargos) {
        this(base, recargos, null);
    }

    public Dinero subtotal() {
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
        Dinero sub = subtotal();
        if (descuento == null) {
            return sub;
        }
        BigDecimal montoDescuento = sub.monto()
            .multiply(descuento.porcentaje())
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        return new Dinero(sub.monto().subtract(montoDescuento), base.codigoMoneda());
    }
}
