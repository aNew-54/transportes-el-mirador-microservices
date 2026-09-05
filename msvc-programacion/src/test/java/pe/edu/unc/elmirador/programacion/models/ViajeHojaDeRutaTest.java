package pe.edu.unc.elmirador.programacion.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import pe.edu.unc.elmirador.programacion.exceptions.TransicionDeViajeInvalidaException;
import pe.edu.unc.elmirador.programacion.models.entity.Viaje;
import pe.edu.unc.elmirador.programacion.models.vo.AsignacionDeRecursos;
import pe.edu.unc.elmirador.programacion.models.vo.Carga;
import pe.edu.unc.elmirador.programacion.models.vo.HojaDeRuta;
import pe.edu.unc.elmirador.programacion.models.vo.Parada;
import pe.edu.unc.elmirador.programacion.models.vo.Ruta;
import pe.edu.unc.elmirador.programacion.models.vo.TipoDeCarga;
import pe.edu.unc.elmirador.programacion.models.vo.Ubicacion;
import pe.edu.unc.elmirador.programacion.models.vo.VentanaDeTiempo;

/**
 * Contrato 4. La regla de que un viaje sin despachar todavia no tiene hoja de ruta ejecutable vive en
 * el agregado y no en el controlador: el transporte HTTP no decide reglas.
 */
class ViajeHojaDeRutaTest {

    private static final OffsetDateTime BASE =
            OffsetDateTime.of(2026, 9, 10, 6, 0, 0, 0, ZoneOffset.of("-05:00"));

    private Viaje viajePlanificado() {
        return Viaje.planificar(
                "V-001",
                new Ruta("Cajamarca", "Trujillo", "COSTA_NORTE"),
                new VentanaDeTiempo(BASE, BASE.plusHours(12)),
                new Carga("ORD-01", 8500, new BigDecimal("24.5"), TipoDeCarga.GENERAL, 1));
    }

    private HojaDeRuta hojaConUbicacion() {
        return HojaDeRuta.de(new Parada(
                1, Parada.CARGA, "ORD-01",
                new Ubicacion("Jr. Ayacucho 450", "Cajamarca", "Almacen 2", "+51 976 000 111"),
                BASE.plusMinutes(30)));
    }

    @Test
    @DisplayName("un viaje planificado aun no tiene hoja de ruta ejecutable")
    void planificadoNoTieneHoja() {
        assertThatThrownBy(() -> viajePlanificado().hojaDeRutaEjecutable())
                .isInstanceOf(TransicionDeViajeInvalidaException.class)
                .hasMessageContaining("PLANIFICADO");
    }

    @Test
    @DisplayName("un viaje cancelado tampoco")
    void canceladoNoTieneHoja() {
        Viaje viaje = viajePlanificado();
        viaje.cancelar();

        assertThatThrownBy(viaje::hojaDeRutaEjecutable)
                .isInstanceOf(TransicionDeViajeInvalidaException.class)
                .hasMessageContaining("CANCELADO");
    }

    @Test
    @DisplayName("un viaje programado la devuelve entera, con la ubicacion que el contrato 4 pide")
    void programadoDevuelveLaHoja() {
        Viaje viaje = viajePlanificado();
        viaje.asignarRecursos(AsignacionDeRecursos.de("U-01", "C-01"));
        HojaDeRuta hoja = hojaConUbicacion();
        viaje.confirmarProgramacion(hoja);

        HojaDeRuta devuelta = viaje.hojaDeRutaEjecutable();

        assertThat(devuelta).isEqualTo(hoja);
        assertThat(devuelta.paradas().getFirst().ubicacion().distrito()).isEqualTo("Cajamarca");
        assertThat(devuelta.paradas().getFirst().ubicacion().contacto()).isEqualTo("+51 976 000 111");
    }
}
