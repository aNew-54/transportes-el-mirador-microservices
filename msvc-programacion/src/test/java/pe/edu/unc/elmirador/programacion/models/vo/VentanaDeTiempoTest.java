package pe.edu.unc.elmirador.programacion.models.vo;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VentanaDeTiempoTest {

    private final OffsetDateTime base = OffsetDateTime.of(2026, 9, 10, 8, 0, 0, 0, ZoneOffset.ofHours(-5));

    @Test
    @DisplayName("D5 - seSolapaCon detecta solape parcial entre dos ventanas")
    void seSolapaCon_solapeParcial() {
        VentanaDeTiempo v1 = new VentanaDeTiempo(base, base.plusHours(4));
        VentanaDeTiempo v2 = new VentanaDeTiempo(base.plusHours(2), base.plusHours(6));

        assertTrue(v1.seSolapaCon(v2));
        assertTrue(v2.seSolapaCon(v1));
    }

    @Test
    @DisplayName("D5 - seSolapaCon detecta contencion total de una ventana dentro de otra")
    void seSolapaCon_contencionTotal() {
        VentanaDeTiempo v1 = new VentanaDeTiempo(base, base.plusHours(8));
        VentanaDeTiempo v2 = new VentanaDeTiempo(base.plusHours(2), base.plusHours(4));

        assertTrue(v1.seSolapaCon(v2));
        assertTrue(v2.seSolapaCon(v1));
    }

    @Test
    @DisplayName("D5 - seSolapaCon no considera solapadas dos ventanas que solo se tocan en el borde")
    void seSolapaCon_bordesQueSeTocan() {
        VentanaDeTiempo v1 = new VentanaDeTiempo(base, base.plusHours(4));
        VentanaDeTiempo v2 = new VentanaDeTiempo(base.plusHours(4), base.plusHours(8));

        assertFalse(v1.seSolapaCon(v2));
        assertFalse(v2.seSolapaCon(v1));
    }

    @Test
    @DisplayName("D5 - seSolapaCon devuelve false para dos ventanas disjuntas separadas en el tiempo")
    void seSolapaCon_disjuntas() {
        VentanaDeTiempo v1 = new VentanaDeTiempo(base, base.plusHours(2));
        VentanaDeTiempo v2 = new VentanaDeTiempo(base.plusHours(4), base.plusHours(6));

        assertFalse(v1.seSolapaCon(v2));
        assertFalse(v2.seSolapaCon(v1));
    }

    @Test
    @DisplayName("D5 - constructor rechaza ventana donde 'hasta' es igual o anterior a 'desde'")
    void constructor_rechazaRangoInvalido() {
        assertThrows(IllegalArgumentException.class, () ->
            new VentanaDeTiempo(base, base)
        );
        assertThrows(IllegalArgumentException.class, () ->
            new VentanaDeTiempo(base, base.minusHours(1))
        );
    }

    @Test
    @DisplayName("D1 / D2 - constructor rechaza argumentos nulos")
    void constructor_rechazaNulos() {
        assertThrows(IllegalArgumentException.class, () ->
            new VentanaDeTiempo(null, base.plusHours(2))
        );
        assertThrows(IllegalArgumentException.class, () ->
            new VentanaDeTiempo(base, null)
        );
    }

    @Test
    @DisplayName("D2 - seSolapaCon rechaza ventana nula")
    void seSolapaCon_rechazaNulo() {
        VentanaDeTiempo v = new VentanaDeTiempo(base, base.plusHours(2));
        assertThrows(IllegalArgumentException.class, () -> v.seSolapaCon(null));
    }
}
