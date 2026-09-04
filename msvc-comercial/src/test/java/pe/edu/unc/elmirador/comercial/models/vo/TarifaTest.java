package pe.edu.unc.elmirador.comercial.models.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TarifaTest {

    @Test
    @DisplayName("Tarifa.total(): Base 1000, recargo 10% y descuento 15% resulta en subtotal 1100.00 y total exacto 935.00")
    void debeCalcularTotalConRecargoYDescuentoOrdenNormativo() {
        Dinero base = Dinero.de("1000.00", "PEN");
        Recargo recargo = new Recargo(TipoDeRecargo.COMBUSTIBLE, new BigDecimal("10"));
        Descuento descuento = new Descuento(new BigDecimal("15"), "GERENCIA_GENERAL");

        Tarifa tarifa = new Tarifa(base, List.of(recargo), descuento);

        assertThat(tarifa.subtotal().monto()).isEqualByComparingTo("1100.00");
        assertThat(tarifa.total().monto()).isEqualByComparingTo("935.00");
        assertThat(tarifa.total().codigoMoneda()).isEqualTo("PEN");
    }

    @Test
    @DisplayName("Tarifa.total(): Sin recargos y sin descuento el total es exactamente igual a la base de 1000.00")
    void debeCalcularTotalSinRecargoYSinDescuento() {
        Dinero base = Dinero.de("1000.00", "PEN");
        Tarifa tarifa = new Tarifa(base, List.of(), null);

        assertThat(tarifa.subtotal().monto()).isEqualByComparingTo("1000.00");
        assertThat(tarifa.total().monto()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Tarifa.total(): Base 1000, recargo 10% y descuento 10% da 990.00 demostrando aplicacion sobre el subtotal")
    void debeAplicarDescuentoSobreSubtotalYNoSobreBase() {
        Dinero base = Dinero.de("1000.00", "PEN");
        Recargo recargo = new Recargo(TipoDeRecargo.COMBUSTIBLE, new BigDecimal("10"));
        Descuento descuento = new Descuento(new BigDecimal("10"), "GERENCIA_COMERCIAL");

        Tarifa tarifa = new Tarifa(base, List.of(recargo), descuento);

        assertThat(tarifa.subtotal().monto()).isEqualByComparingTo("1100.00");
        assertThat(tarifa.total().monto()).isEqualByComparingTo("990.00");
    }

    @Test
    @DisplayName("Tarifa.total(): Multiples recargos se suman sobre la tarifa base")
    void debeSumarMultiplesRecargosSobreBase() {
        Dinero base = Dinero.de("1000.00", "PEN");
        Recargo r1 = new Recargo(TipoDeRecargo.COMBUSTIBLE, new BigDecimal("10"));
        Recargo r2 = new Recargo(TipoDeRecargo.PELIGROSIDAD, new BigDecimal("5"));

        Tarifa tarifa = new Tarifa(base, List.of(r1, r2), null);

        assertThat(tarifa.subtotal().monto()).isEqualByComparingTo("1150.00");
        assertThat(tarifa.total().monto()).isEqualByComparingTo("1150.00");
    }

    @Test
    @DisplayName("Constructor de Tarifa rechaza base nula o lista de recargos nula")
    void debeRechazarParametrosNulosEnTarifa() {
        assertThatThrownBy(() -> new Tarifa(null, List.of(), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("base es obligatoria");

        assertThatThrownBy(() -> new Tarifa(Dinero.de("100.00", "PEN"), null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("recargos no puede ser nula");
    }
}
