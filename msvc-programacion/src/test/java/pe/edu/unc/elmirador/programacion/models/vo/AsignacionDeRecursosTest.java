package pe.edu.unc.elmirador.programacion.models.vo;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AsignacionDeRecursosTest {

    @Test
    @DisplayName("VIA-01 - Asignacion con un conductor sin relevo es completa")
    void asignacionUnConductor_esCompleta() {
        AsignacionDeRecursos asignacion = new AsignacionDeRecursos("UNI-001", List.of("CON-001"), false);
        assertTrue(asignacion.esCompleta());
    }

    @Test
    @DisplayName("VIA-01 - Asignacion con dos conductores y conRelevo=false lanza IllegalArgumentException")
    void asignacionDosConductoresSinRelevo_lanza() {
        assertThrows(IllegalArgumentException.class, () ->
            new AsignacionDeRecursos("UNI-001", List.of("CON-001", "CON-002"), false)
        );
    }

    @Test
    @DisplayName("VIA-01 - Asignacion con dos conductores y conRelevo=true es permitida y completa")
    void asignacionDosConductoresConRelevo_esPermitida() {
        AsignacionDeRecursos asignacion = new AsignacionDeRecursos("UNI-001", List.of("CON-001", "CON-002"), true);
        assertTrue(asignacion.esCompleta());
    }

    @Test
    @DisplayName("VIA-01 - Asignacion con tres conductores lanza siempre IllegalArgumentException")
    void asignacionTresConductores_lanzaSiempre() {
        assertThrows(IllegalArgumentException.class, () ->
            new AsignacionDeRecursos("UNI-001", List.of("CON-001", "CON-002", "CON-003"), false)
        );
        assertThrows(IllegalArgumentException.class, () ->
            new AsignacionDeRecursos("UNI-001", List.of("CON-001", "CON-002", "CON-003"), true)
        );
    }

    @Test
    @DisplayName("VIA-01 - Asignacion sin unidad no es completa")
    void asignacionSinUnidad_noEsCompleta() {
        AsignacionDeRecursos asignacionNula = new AsignacionDeRecursos(null, List.of("CON-001"), false);
        assertFalse(asignacionNula.esCompleta());

        AsignacionDeRecursos asignacionVacia = new AsignacionDeRecursos("   ", List.of("CON-001"), false);
        assertFalse(asignacionVacia.esCompleta());
    }

    @Test
    @DisplayName("VIA-01 - Asignacion sin conductores no es completa")
    void asignacionSinConductores_noEsCompleta() {
        AsignacionDeRecursos asignacion = new AsignacionDeRecursos("UNI-001", List.of(), false);
        assertFalse(asignacion.esCompleta());

        AsignacionDeRecursos asignacionNula = new AsignacionDeRecursos("UNI-001", null, false);
        assertFalse(asignacionNula.esCompleta());
    }

    @Test
    @DisplayName("D2 - Conductor con id nulo o en blanco lanza IllegalArgumentException")
    void asignacionConductorInvalido_lanza() {
        assertThrows(IllegalArgumentException.class, () ->
            new AsignacionDeRecursos("UNI-001", List.of(""), false)
        );
        assertThrows(IllegalArgumentException.class, () ->
            new AsignacionDeRecursos("UNI-001", List.of("   "), false)
        );
    }
}
