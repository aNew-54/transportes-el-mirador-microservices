package pe.edu.unc.elmirador.unidades.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.unidades.exceptions.ExistenciasNegativasException;
import pe.edu.unc.elmirador.unidades.models.entity.Repuesto;
import pe.edu.unc.elmirador.unidades.models.vo.Dinero;

class RepuestoTest {

    private Repuesto crearRepuesto(int existencias, int stockMinimo) {
        return new Repuesto(
                "REP-001",
                "FILT-01",
                "Filtro de aceite primario",
                existencias,
                stockMinimo,
                new Dinero(new BigDecimal("45.00"), "PEN"));
    }

    // =========================================================================
    // INVARIANTE REP-01
    // "Las existencias nunca pueden ser negativas."
    // ajustarInventario(-n) que deja negativo lanza y no altera las existencias;
    // dejar exactamente cero no lanza.
    // =========================================================================

    @Test
    @DisplayName("REP-01: ajustarInventario que dejaria existencias negativas lanza ExistenciasNegativasException y no altera existencias")
    void ajustarInventarioDejandoExistenciasNegativasLanzaExcepcionYConservaExistencias_REP01() {
        Repuesto repuesto = crearRepuesto(10, 3);

        // Invariante REP-01: existencias actuales son 10, restar 11 dejaria -1
        assertThatThrownBy(() -> repuesto.ajustarInventario(-11))
                .isInstanceOf(ExistenciasNegativasException.class)
                .hasMessageContaining("REP-01");

        // El estado debe permanecer intacto
        assertThat(repuesto.getExistencias()).isEqualTo(10);
    }

    @Test
    @DisplayName("REP-01: ajustarInventario dejando exactamente cero existencias no lanza excepcion")
    void ajustarInventarioDejandoExactamenteCeroNoLanza_REP01() {
        Repuesto repuesto = crearRepuesto(5, 2);

        // Dejar exactamente cero
        repuesto.ajustarInventario(-5);

        assertThat(repuesto.getExistencias()).isZero();
    }

    @Test
    @DisplayName("ajustarInventario incrementa existencias ante entrada positiva")
    void ajustarInventarioIncrementaExistencias() {
        Repuesto repuesto = crearRepuesto(10, 3);

        repuesto.ajustarInventario(15);

        assertThat(repuesto.getExistencias()).isEqualTo(25);
    }

    @Test
    @DisplayName("REP-01: constructor con existencias iniciales negativas lanza ExistenciasNegativasException")
    void crearRepuestoConExistenciasNegativasLanzaExcepcion_REP01() {
        // Invariante REP-01
        assertThatThrownBy(() -> new Repuesto(
                "REP-002",
                "LLANT-01",
                "Llanta 295/80R22.5",
                -1,
                2,
                new Dinero(new BigDecimal("1200.00"), "PEN")))
                .isInstanceOf(ExistenciasNegativasException.class)
                .hasMessageContaining("REP-01");
    }

    // =========================================================================
    // Comportamiento requiereReposicion (existencias <= stockMinimo)
    // =========================================================================

    @Test
    @DisplayName("requiereReposicion retorna true cuando las existencias son iguales o menores al stock minimo")
    void requiereReposicionRetornaTrueCuandoExistenciasSonMenoresOIgualesAlStockMinimo() {
        Repuesto repuestoEnStockMinimo = crearRepuesto(5, 5);
        assertThat(repuestoEnStockMinimo.requiereReposicion()).isTrue();

        Repuesto repuestoBajoStockMinimo = crearRepuesto(4, 5);
        assertThat(repuestoBajoStockMinimo.requiereReposicion()).isTrue();
    }

    @Test
    @DisplayName("requiereReposicion retorna false cuando las existencias superan el stock minimo")
    void requiereReposicionRetornaFalseCuandoExistenciasSuperanStockMinimo() {
        Repuesto repuestoConStockSuficiente = crearRepuesto(6, 5);
        assertThat(repuestoConStockSuficiente.requiereReposicion()).isFalse();
    }
}
