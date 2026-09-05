package pe.edu.unc.elmirador.ejecucion.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.ejecucion.exceptions.EjecucionEntregadaException;
import pe.edu.unc.elmirador.ejecucion.models.entity.ConformidadDeEntrega;
import pe.edu.unc.elmirador.ejecucion.models.entity.EjecucionDeViaje;
import pe.edu.unc.elmirador.ejecucion.models.entity.Parada;
import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoConformidad;
import pe.edu.unc.elmirador.ejecucion.models.vo.ResultadoDeCheckList;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransbordoTest {

    private static final ZoneOffset LIMA = ZoneOffset.ofHours(-5);
    private static final OffsetDateTime T08_00 = OffsetDateTime.of(2026, 9, 10, 8, 0, 0, 0, LIMA);
    private static final OffsetDateTime T12_00 = OffsetDateTime.of(2026, 9, 10, 12, 0, 0, 0, LIMA);

    private EjecucionDeViaje crearEjecucionEnRuta() {
        Parada parada = new Parada(1, "ORD-001", "Destino");
        EjecucionDeViaje ejecucion = EjecucionDeViaje.crear("VIA-2026-00045", "UNI-001", List.of("CON-001"), List.of(parada));
        ejecucion.registrarCheckList(ResultadoDeCheckList.aprobado(T08_00));
        ejecucion.iniciar(T08_00);
        return ejecucion;
    }

    @Test
    @DisplayName("[EJV-05] Transbordo cambia la unidad ejecutora, conserva el viajeId y registra la unidad anterior")
    void transbordoCambiaUnidadYConservaViajeId_EJV05() {
        EjecucionDeViaje ejecucion = crearEjecucionEnRuta();

        ejecucion.transbordar("UNI-002");

        assertThat(ejecucion.getViajeId())
                .as("[EJV-05] El viajeId debe permanecer inmutable")
                .isEqualTo("VIA-2026-00045");
        assertThat(ejecucion.getUnidadEjecutoraId())
                .as("[EJV-05] La unidad ejecutora debe actualizarse a la nueva unidad")
                .isEqualTo("UNI-002");
        assertThat(ejecucion.getUnidadesAnteriores())
                .as("[EJV-05] La unidad anterior debe quedar registrada en el historial")
                .containsExactly("UNI-001");
    }

    @Test
    @DisplayName("[EJV-05] Transbordos sucesivos conservan el historial ordenado de unidades anteriores")
    void transbordosSucesivosConservanHistorial_EJV05() {
        EjecucionDeViaje ejecucion = crearEjecucionEnRuta();

        ejecucion.transbordar("UNI-002");
        ejecucion.transbordar("UNI-003");

        assertThat(ejecucion.getViajeId()).isEqualTo("VIA-2026-00045");
        assertThat(ejecucion.getUnidadEjecutoraId()).isEqualTo("UNI-003");
        assertThat(ejecucion.getUnidadesAnteriores()).containsExactly("UNI-001", "UNI-002");
    }

    @Test
    @DisplayName("[EJV-05] Transbordo sobre ejecucion ENTREGADA lanza EjecucionEntregadaException")
    void transbordoSobreEjecucionEntregadaLanzaExcepcion_EJV05() {
        EjecucionDeViaje ejecucion = crearEjecucionEnRuta();
        ConformidadDeEntrega conformidad = new ConformidadDeEntrega("CONF-01", "ORD-001", EstadoConformidad.FIRMADA, "Receptor", T12_00, "OK");
        ejecucion.registrarConformidad(1, conformidad);
        ejecucion.marcarEntregada(T12_00);

        assertThatThrownBy(() -> ejecucion.transbordar("UNI-999"))
                .as("[EJV-05] No se puede transbordar una ejecucion entregada")
                .isInstanceOf(EjecucionEntregadaException.class)
                .hasMessageContaining("No se puede realizar un transbordo");
    }

    @Test
    @DisplayName("[EJV-05] Transbordo sobre ejecucion CERRADA lanza EjecucionEntregadaException")
    void transbordoSobreEjecucionCerradaLanzaExcepcion_EJV05() {
        EjecucionDeViaje ejecucion = crearEjecucionEnRuta();
        ConformidadDeEntrega conformidad = new ConformidadDeEntrega("CONF-01", "ORD-001", EstadoConformidad.FIRMADA, "Receptor", T12_00, "OK");
        ejecucion.registrarConformidad(1, conformidad);
        ejecucion.marcarEntregada(T12_00);
        ejecucion.cerrar(184320, false, Set.of("CON-001"), Set.of());

        assertThatThrownBy(() -> ejecucion.transbordar("UNI-999"))
                .as("[EJV-05] No se puede transbordar una ejecucion cerrada")
                .isInstanceOf(EjecucionEntregadaException.class);
    }

    @Test
    @DisplayName("Transbordo a la misma unidad actual lanza IllegalArgumentException")
    void transbordoAMismaUnidadLanzaExcepcion() {
        EjecucionDeViaje ejecucion = crearEjecucionEnRuta();

        assertThatThrownBy(() -> ejecucion.transbordar("UNI-001"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("distinta a la unidad actual");
    }

    @Test
    @DisplayName("Transbordo con identificador nulo o vacio lanza IllegalArgumentException")
    void transbordoConIdentificadorInvalidoLanzaExcepcion() {
        EjecucionDeViaje ejecucion = crearEjecucionEnRuta();

        assertThatThrownBy(() -> ejecucion.transbordar(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ejecucion.transbordar("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
