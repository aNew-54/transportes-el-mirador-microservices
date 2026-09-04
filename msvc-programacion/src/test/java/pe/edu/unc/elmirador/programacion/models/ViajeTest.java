package pe.edu.unc.elmirador.programacion.models;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.programacion.exceptions.AsignacionIncompletaException;
import pe.edu.unc.elmirador.programacion.exceptions.DominioProgramacionException;
import pe.edu.unc.elmirador.programacion.exceptions.TransicionDeViajeInvalidaException;
import pe.edu.unc.elmirador.programacion.exceptions.ViajeDespachadoException;
import pe.edu.unc.elmirador.programacion.models.entity.Viaje;
import pe.edu.unc.elmirador.programacion.models.vo.AsignacionDeRecursos;
import pe.edu.unc.elmirador.programacion.models.vo.Capacidad;
import pe.edu.unc.elmirador.programacion.models.vo.Carga;
import pe.edu.unc.elmirador.programacion.models.vo.ClausulaDeConsolidacion;
import pe.edu.unc.elmirador.programacion.models.vo.EstadoDeViaje;
import pe.edu.unc.elmirador.programacion.models.vo.HojaDeRuta;
import pe.edu.unc.elmirador.programacion.models.vo.Parada;
import pe.edu.unc.elmirador.programacion.models.vo.Ruta;
import pe.edu.unc.elmirador.programacion.models.vo.TipoDeCarga;
import pe.edu.unc.elmirador.programacion.models.vo.VentanaDeTiempo;

import static org.junit.jupiter.api.Assertions.*;

class ViajeTest {

    private final OffsetDateTime base = OffsetDateTime.of(2026, 9, 10, 8, 0, 0, 0, ZoneOffset.ofHours(-5));
    private final Ruta rutaEjemplo = new Ruta("Cajamarca", "Trujillo", "COSTA_NORTE");
    private final VentanaDeTiempo ventanaEjemplo = new VentanaDeTiempo(base, base.plusHours(12));
    private final Carga cargaEjemplo = new Carga(
            "ORD-001",
            5000,
            BigDecimal.valueOf(15.5),
            TipoDeCarga.PALETIZADA,
            1
    );
    private final HojaDeRuta hojaDeRutaEjemplo = new HojaDeRuta(List.of(
            Parada.de(1, "CARGA", "ORD-001"),
            Parada.de(2, "DESCARGA", "ORD-001")
    ));

    private Viaje crearViajePlanificado() {
        return Viaje.planificar("VIA-001", rutaEjemplo, ventanaEjemplo, cargaEjemplo);
    }

    @Test
    @DisplayName("VIA-01 - confirmarProgramacion sin unidad asignada lanza AsignacionIncompletaException")
    void confirmarProgramacion_sinUnidadLanza() {
        Viaje viaje = crearViajePlanificado();
        AsignacionDeRecursos asignacionSinUnidad = new AsignacionDeRecursos(null, List.of("CON-001"), false);
        viaje.asignarRecursos(asignacionSinUnidad);

        assertThrows(AsignacionIncompletaException.class, () ->
            viaje.confirmarProgramacion(hojaDeRutaEjemplo)
        );
        assertEquals(EstadoDeViaje.PLANIFICADO, viaje.estado());
    }

    @Test
    @DisplayName("VIA-01 - confirmarProgramacion sin conductores asignados lanza AsignacionIncompletaException")
    void confirmarProgramacion_sinConductoresLanza() {
        Viaje viaje = crearViajePlanificado();
        AsignacionDeRecursos asignacionSinConductor = new AsignacionDeRecursos("UNI-001", List.of(), false);
        viaje.asignarRecursos(asignacionSinConductor);

        assertThrows(AsignacionIncompletaException.class, () ->
            viaje.confirmarProgramacion(hojaDeRutaEjemplo)
        );
        assertEquals(EstadoDeViaje.PLANIFICADO, viaje.estado());
    }

    @Test
    @DisplayName("VIA-01 - confirmarProgramacion sin recursos asignados (nulo) lanza AsignacionIncompletaException")
    void confirmarProgramacion_recursosNulosLanza() {
        Viaje viaje = crearViajePlanificado();

        assertThrows(AsignacionIncompletaException.class, () ->
            viaje.confirmarProgramacion(hojaDeRutaEjemplo)
        );
        assertEquals(EstadoDeViaje.PLANIFICADO, viaje.estado());
    }

    @Test
    @DisplayName("VIA-01 - confirmarProgramacion con asignacion completa pasa a PROGRAMADO")
    void confirmarProgramacion_conAsignacionCompletaPasaAProgramado() {
        Viaje viaje = crearViajePlanificado();
        AsignacionDeRecursos asignacion = AsignacionDeRecursos.de("UNI-001", "CON-001");
        viaje.asignarRecursos(asignacion);

        viaje.confirmarProgramacion(hojaDeRutaEjemplo);

        assertEquals(EstadoDeViaje.PROGRAMADO, viaje.estado());
        assertNotNull(viaje.hojaDeRuta());
    }

    @Test
    @DisplayName("VIA-07 - consolidarOrden sobre un viaje DESPACHADO lanza ViajeDespachadoException")
    void consolidarOrden_viajeDespachadoLanza() {
        Viaje viaje = crearViajePlanificado();
        viaje.asignarRecursos(AsignacionDeRecursos.de("UNI-001", "CON-001"));
        viaje.confirmarProgramacion(hojaDeRutaEjemplo);
        viaje.autorizarDespacho();
        assertEquals(EstadoDeViaje.DESPACHADO, viaje.estado());

        Carga nuevaCarga = new Carga("ORD-002", 2000, BigDecimal.valueOf(5.0), TipoDeCarga.PALETIZADA, 2);
        Capacidad capacidad = new Capacidad(10000, BigDecimal.valueOf(30.0));
        ClausulaDeConsolidacion clausula = ClausulaDeConsolidacion.consolidacionPermitida();

        assertThrows(ViajeDespachadoException.class, () ->
            viaje.consolidarOrden(nuevaCarga, rutaEjemplo, ventanaEjemplo, clausula, capacidad)
        );
    }

    @Test
    @DisplayName("Viaje - autorizarDespacho pasa de PROGRAMADO a DESPACHADO")
    void autorizarDespacho_desdeProgramado() {
        Viaje viaje = crearViajePlanificado();
        viaje.asignarRecursos(AsignacionDeRecursos.de("UNI-001", "CON-001"));
        viaje.confirmarProgramacion(hojaDeRutaEjemplo);

        viaje.autorizarDespacho();

        assertEquals(EstadoDeViaje.DESPACHADO, viaje.estado());
    }

    @Test
    @DisplayName("Viaje - autorizarDespacho desde PLANIFICADO lanza TransicionDeViajeInvalidaException")
    void autorizarDespacho_desdePlanificadoLanza() {
        Viaje viaje = crearViajePlanificado();

        assertThrows(TransicionDeViajeInvalidaException.class, viaje::autorizarDespacho);
    }

    @Test
    @DisplayName("Viaje - cancelar pasa a CANCELADO desde PLANIFICADO o PROGRAMADO")
    void cancelar_desdePlanificadoYProgramado() {
        Viaje viaje1 = crearViajePlanificado();
        viaje1.cancelar();
        assertEquals(EstadoDeViaje.CANCELADO, viaje1.estado());

        Viaje viaje2 = crearViajePlanificado();
        viaje2.asignarRecursos(AsignacionDeRecursos.de("UNI-001", "CON-001"));
        viaje2.confirmarProgramacion(hojaDeRutaEjemplo);
        viaje2.cancelar();
        assertEquals(EstadoDeViaje.CANCELADO, viaje2.estado());
    }

    @Test
    @DisplayName("Viaje - cancelar desde DESPACHADO lanza TransicionDeViajeInvalidaException")
    void cancelar_desdeDespachadoLanza() {
        Viaje viaje = crearViajePlanificado();
        viaje.asignarRecursos(AsignacionDeRecursos.de("UNI-001", "CON-001"));
        viaje.confirmarProgramacion(hojaDeRutaEjemplo);
        viaje.autorizarDespacho();

        assertThrows(TransicionDeViajeInvalidaException.class, viaje::cancelar);
    }

    @Test
    @DisplayName("Viaje - asignarRecursos sobre DESPACHADO o CANCELADO lanza excepcion")
    void asignarRecursos_estadosTerminalesLanza() {
        Viaje viajeDespachado = crearViajePlanificado();
        viajeDespachado.asignarRecursos(AsignacionDeRecursos.de("UNI-001", "CON-001"));
        viajeDespachado.confirmarProgramacion(hojaDeRutaEjemplo);
        viajeDespachado.autorizarDespacho();

        assertThrows(ViajeDespachadoException.class, () ->
            viajeDespachado.asignarRecursos(AsignacionDeRecursos.de("UNI-002", "CON-002"))
        );

        Viaje viajeCancelado = crearViajePlanificado();
        viajeCancelado.cancelar();

        assertThrows(DominioProgramacionException.class, () ->
            viajeCancelado.asignarRecursos(AsignacionDeRecursos.de("UNI-002", "CON-002"))
        );
    }

    @Test
    @DisplayName("D2 - confirmarProgramacion con HojaDeRuta nula lanza IllegalArgumentException")
    void confirmarProgramacion_hojaDeRutaNulaLanza() {
        Viaje viaje = crearViajePlanificado();
        viaje.asignarRecursos(AsignacionDeRecursos.de("UNI-001", "CON-001"));

        assertThrows(IllegalArgumentException.class, () ->
            viaje.confirmarProgramacion(null)
        );
    }
}
