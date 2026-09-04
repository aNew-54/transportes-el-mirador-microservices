package pe.edu.unc.elmirador.programacion.models.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.programacion.exceptions.TransicionDeViajeInvalidaException;

import static org.junit.jupiter.api.Assertions.*;

class EstadoDeViajeTest {

    @Test
    @DisplayName("EstadoDeViaje - Transiciones permitidas desde PLANIFICADO")
    void transicionesPermitidas_desdePlanificado() {
        assertTrue(EstadoDeViaje.PLANIFICADO.puedeTransicionarA(EstadoDeViaje.PROGRAMADO));
        assertTrue(EstadoDeViaje.PLANIFICADO.puedeTransicionarA(EstadoDeViaje.CANCELADO));
    }

    @Test
    @DisplayName("EstadoDeViaje - Transiciones permitidas desde PROGRAMADO")
    void transicionesPermitidas_desdeProgramado() {
        assertTrue(EstadoDeViaje.PROGRAMADO.puedeTransicionarA(EstadoDeViaje.DESPACHADO));
        assertTrue(EstadoDeViaje.PROGRAMADO.puedeTransicionarA(EstadoDeViaje.CANCELADO));
    }

    @Test
    @DisplayName("EstadoDeViaje - PLANIFICADO a DESPACHADO es prohibida (no se salta estados)")
    void transicionProhibida_planificadoADespachado() {
        assertFalse(EstadoDeViaje.PLANIFICADO.puedeTransicionarA(EstadoDeViaje.DESPACHADO));
        assertThrows(TransicionDeViajeInvalidaException.class, () ->
            EstadoDeViaje.PLANIFICADO.validarTransicion(EstadoDeViaje.DESPACHADO)
        );
    }

    @Test
    @DisplayName("EstadoDeViaje - PLANIFICADO a PLANIFICADO es prohibida")
    void transicionProhibida_planificadoAPlanificado() {
        assertFalse(EstadoDeViaje.PLANIFICADO.puedeTransicionarA(EstadoDeViaje.PLANIFICADO));
    }

    @Test
    @DisplayName("EstadoDeViaje - PROGRAMADO a PLANIFICADO es prohibida")
    void transicionProhibida_programadoAPlanificado() {
        assertFalse(EstadoDeViaje.PROGRAMADO.puedeTransicionarA(EstadoDeViaje.PLANIFICADO));
    }

    @Test
    @DisplayName("EstadoDeViaje - PROGRAMADO a PROGRAMADO es prohibida")
    void transicionProhibida_programadoAProgramado() {
        assertFalse(EstadoDeViaje.PROGRAMADO.puedeTransicionarA(EstadoDeViaje.PROGRAMADO));
    }

    @Test
    @DisplayName("EstadoDeViaje - DESPACHADO es estado terminal hacia PLANIFICADO")
    void transicionProhibida_despachadoAPlanificado() {
        assertFalse(EstadoDeViaje.DESPACHADO.puedeTransicionarA(EstadoDeViaje.PLANIFICADO));
    }

    @Test
    @DisplayName("EstadoDeViaje - DESPACHADO es estado terminal hacia PROGRAMADO")
    void transicionProhibida_despachadoAProgramado() {
        assertFalse(EstadoDeViaje.DESPACHADO.puedeTransicionarA(EstadoDeViaje.PROGRAMADO));
    }

    @Test
    @DisplayName("EstadoDeViaje - DESPACHADO es estado terminal hacia DESPACHADO")
    void transicionProhibida_despachadoADespachado() {
        assertFalse(EstadoDeViaje.DESPACHADO.puedeTransicionarA(EstadoDeViaje.DESPACHADO));
    }

    @Test
    @DisplayName("EstadoDeViaje - DESPACHADO es estado terminal hacia CANCELADO")
    void transicionProhibida_despachadoACancelado() {
        assertFalse(EstadoDeViaje.DESPACHADO.puedeTransicionarA(EstadoDeViaje.CANCELADO));
    }

    @Test
    @DisplayName("EstadoDeViaje - CANCELADO es estado terminal hacia PLANIFICADO")
    void transicionProhibida_canceladoAPlanificado() {
        assertFalse(EstadoDeViaje.CANCELADO.puedeTransicionarA(EstadoDeViaje.PLANIFICADO));
    }

    @Test
    @DisplayName("EstadoDeViaje - CANCELADO es estado terminal hacia PROGRAMADO")
    void transicionProhibida_canceladoAProgramado() {
        assertFalse(EstadoDeViaje.CANCELADO.puedeTransicionarA(EstadoDeViaje.PROGRAMADO));
    }

    @Test
    @DisplayName("EstadoDeViaje - CANCELADO es estado terminal hacia DESPACHADO")
    void transicionProhibida_canceladoADespachado() {
        assertFalse(EstadoDeViaje.CANCELADO.puedeTransicionarA(EstadoDeViaje.DESPACHADO));
    }

    @Test
    @DisplayName("EstadoDeViaje - CANCELADO es estado terminal hacia CANCELADO")
    void transicionProhibida_canceladoACancelado() {
        assertFalse(EstadoDeViaje.CANCELADO.puedeTransicionarA(EstadoDeViaje.CANCELADO));
    }

    @Test
    @DisplayName("D2 - puedeTransicionarA con estado nulo lanza IllegalArgumentException")
    void transicion_conNuloLanza() {
        assertThrows(IllegalArgumentException.class, () ->
            EstadoDeViaje.PLANIFICADO.puedeTransicionarA(null)
        );
    }
}
