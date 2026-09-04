package pe.edu.unc.elmirador.programacion.models;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.programacion.exceptions.DominioProgramacionException;
import pe.edu.unc.elmirador.programacion.exceptions.RecursoNoElegibleException;
import pe.edu.unc.elmirador.programacion.exceptions.ReservaSolapadaException;
import pe.edu.unc.elmirador.programacion.models.entity.AgendaDeUnidad;
import pe.edu.unc.elmirador.programacion.models.entity.ReservaDeUnidad;
import pe.edu.unc.elmirador.programacion.models.vo.ElegibilidadDeRecurso;
import pe.edu.unc.elmirador.programacion.models.vo.EstadoDeReserva;
import pe.edu.unc.elmirador.programacion.models.vo.VentanaDeTiempo;

import static org.junit.jupiter.api.Assertions.*;

class AgendaDeUnidadTest {

    private final OffsetDateTime base = OffsetDateTime.of(2026, 9, 10, 8, 0, 0, 0, ZoneOffset.ofHours(-5));

    @Test
    @DisplayName("AGU-01 - Dos reservas solapadas de la misma unidad: la segunda lanza ReservaSolapadaException")
    void reservar_solapadaLanzaExcepcion() {
        AgendaDeUnidad agenda = new AgendaDeUnidad("UNI-001");
        VentanaDeTiempo v1 = new VentanaDeTiempo(base, base.plusHours(4));
        VentanaDeTiempo v2 = new VentanaDeTiempo(base.plusHours(2), base.plusHours(6));

        agenda.reservar("RES-001", v1, ElegibilidadDeRecurso.recursoElegible(), "VIA-001");

        assertThrows(ReservaSolapadaException.class, () ->
            agenda.reservar("RES-002", v2, ElegibilidadDeRecurso.recursoElegible(), "VIA-002")
        );
    }

    @Test
    @DisplayName("AGU-01 - Con la primera reserva LIBERADA no lanza y permite reservar en la misma ventana")
    void reservar_conPrimeraLiberadaNoLanza() {
        AgendaDeUnidad agenda = new AgendaDeUnidad("UNI-001");
        VentanaDeTiempo v1 = new VentanaDeTiempo(base, base.plusHours(4));

        agenda.reservar("RES-001", v1, ElegibilidadDeRecurso.recursoElegible(), "VIA-001");
        agenda.liberar("RES-001");

        assertDoesNotThrow(() ->
            agenda.reservar("RES-002", v1, ElegibilidadDeRecurso.recursoElegible(), "VIA-002")
        );
        assertEquals(2, agenda.reservas().size());
        assertEquals(1, agenda.reservasQueBloquean().size());
    }

    @Test
    @DisplayName("AGU-01 - Ventanas que solo se tocan en el borde no se consideran solapadas")
    void reservar_bordesQueSeTocanPermitidas() {
        AgendaDeUnidad agenda = new AgendaDeUnidad("UNI-001");
        VentanaDeTiempo v1 = new VentanaDeTiempo(base, base.plusHours(4));
        VentanaDeTiempo v2 = new VentanaDeTiempo(base.plusHours(4), base.plusHours(8));

        agenda.reservar("RES-001", v1, ElegibilidadDeRecurso.recursoElegible(), "VIA-001");

        assertDoesNotThrow(() ->
            agenda.reservar("RES-002", v2, ElegibilidadDeRecurso.recursoElegible(), "VIA-002")
        );
        assertEquals(2, agenda.reservasQueBloquean().size());
    }

    @Test
    @DisplayName("AGU-01 - Reserva CONFIRMADA bloquea el recurso ante intento de solape")
    void reservar_reservaConfirmadaBloqueaRecurso() {
        AgendaDeUnidad agenda = new AgendaDeUnidad("UNI-001");
        VentanaDeTiempo v1 = new VentanaDeTiempo(base, base.plusHours(6));
        VentanaDeTiempo v2 = new VentanaDeTiempo(base.plusHours(1), base.plusHours(3));

        agenda.reservar("RES-001", v1, ElegibilidadDeRecurso.recursoElegible(), "VIA-001");
        agenda.confirmar("RES-001");

        assertThrows(ReservaSolapadaException.class, () ->
            agenda.reservar("RES-002", v2, ElegibilidadDeRecurso.recursoElegible(), "VIA-002")
        );
    }

    @Test
    @DisplayName("AGU-01 - Reconfirmar una reserva liberada lanza DominioProgramacionException")
    void confirmar_reservaLiberadaLanza() {
        AgendaDeUnidad agenda = new AgendaDeUnidad("UNI-001");
        VentanaDeTiempo v1 = new VentanaDeTiempo(base, base.plusHours(4));

        agenda.reservar("RES-001", v1, ElegibilidadDeRecurso.recursoElegible(), "VIA-001");
        agenda.liberar("RES-001");

        assertThrows(DominioProgramacionException.class, () ->
            agenda.confirmar("RES-001")
        );
    }

    @Test
    @DisplayName("AGU-02 - Reservar con elegible=false lanza RecursoNoElegibleException e incluye los motivos")
    void reservar_unidadNoElegibleLanza() {
        AgendaDeUnidad agenda = new AgendaDeUnidad("UNI-001");
        VentanaDeTiempo v1 = new VentanaDeTiempo(base, base.plusHours(4));
        ElegibilidadDeRecurso noElegible = ElegibilidadDeRecurso.recursoNoElegible(
            List.of("DOCUMENTO_VENCIDO:SOAT", "MANTENIMIENTO_VENCIDO")
        );

        RecursoNoElegibleException ex = assertThrows(RecursoNoElegibleException.class, () ->
            agenda.reservar("RES-001", v1, noElegible, "VIA-001")
        );

        assertTrue(ex.getMessage().contains("DOCUMENTO_VENCIDO:SOAT"));
        assertTrue(ex.getMessage().contains("MANTENIMIENTO_VENCIDO"));
        assertTrue(ex.getMotivos().contains("DOCUMENTO_VENCIDO:SOAT"));
        assertTrue(ex.getMotivos().contains("MANTENIMIENTO_VENCIDO"));
    }

    @Test
    @DisplayName("AGU-02 / D2 - Reservar con ElegibilidadDeRecurso nula lanza IllegalArgumentException (no se asume elegible)")
    void reservar_elegibilidadNulaLanza() {
        AgendaDeUnidad agenda = new AgendaDeUnidad("UNI-001");
        VentanaDeTiempo v1 = new VentanaDeTiempo(base, base.plusHours(4));

        assertThrows(IllegalArgumentException.class, () ->
            agenda.reservar("RES-001", v1, null, "VIA-001")
        );
    }

    @Test
    @DisplayName("D1 / D2 - Reservar con ventana nula lanza IllegalArgumentException")
    void reservar_ventanaNulaLanza() {
        AgendaDeUnidad agenda = new AgendaDeUnidad("UNI-001");

        assertThrows(IllegalArgumentException.class, () ->
            agenda.reservar("RES-001", null, ElegibilidadDeRecurso.recursoElegible(), "VIA-001")
        );
    }

    @Test
    @DisplayName("D6 - Confirmar o liberar reserva inexistente lanza DominioProgramacionException")
    void operaciones_reservaInexistenteLanza() {
        AgendaDeUnidad agenda = new AgendaDeUnidad("UNI-001");

        assertThrows(DominioProgramacionException.class, () ->
            agenda.confirmar("RES-999")
        );
        assertThrows(DominioProgramacionException.class, () ->
            agenda.liberar("RES-999")
        );
    }

    @Test
    @DisplayName("D8 - reservasQueBloquean retorna solo TENTATIVA y CONFIRMADA")
    void reservasQueBloquean_filtraLiberadas() {
        AgendaDeUnidad agenda = new AgendaDeUnidad("UNI-001");
        VentanaDeTiempo v1 = new VentanaDeTiempo(base, base.plusHours(2));
        VentanaDeTiempo v2 = new VentanaDeTiempo(base.plusHours(2), base.plusHours(4));
        VentanaDeTiempo v3 = new VentanaDeTiempo(base.plusHours(4), base.plusHours(6));

        agenda.reservar("RES-001", v1, ElegibilidadDeRecurso.recursoElegible(), "VIA-001");
        agenda.reservar("RES-002", v2, ElegibilidadDeRecurso.recursoElegible(), "VIA-002");
        agenda.reservar("RES-003", v3, ElegibilidadDeRecurso.recursoElegible(), "VIA-003");

        agenda.confirmar("RES-002");
        agenda.liberar("RES-003");

        List<ReservaDeUnidad> bloqueantes = agenda.reservasQueBloquean();
        assertEquals(2, bloqueantes.size());
        assertTrue(bloqueantes.stream().anyMatch(r -> r.id().equals("RES-001") && r.estado() == EstadoDeReserva.TENTATIVA));
        assertTrue(bloqueantes.stream().anyMatch(r -> r.id().equals("RES-002") && r.estado() == EstadoDeReserva.CONFIRMADA));
        assertFalse(bloqueantes.stream().anyMatch(r -> r.id().equals("RES-003")));
    }
}
