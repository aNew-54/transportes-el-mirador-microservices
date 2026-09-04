package pe.edu.unc.elmirador.programacion.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.programacion.exceptions.CapacidadExcedidaException;
import pe.edu.unc.elmirador.programacion.exceptions.CargaIncompatibleException;
import pe.edu.unc.elmirador.programacion.exceptions.ConsolidacionProhibidaException;
import pe.edu.unc.elmirador.programacion.exceptions.CorredorIncompatibleException;
import pe.edu.unc.elmirador.programacion.models.entity.Viaje;
import pe.edu.unc.elmirador.programacion.models.vo.Capacidad;
import pe.edu.unc.elmirador.programacion.models.vo.Carga;
import pe.edu.unc.elmirador.programacion.models.vo.ClausulaDeConsolidacion;
import pe.edu.unc.elmirador.programacion.models.vo.HojaDeRuta;
import pe.edu.unc.elmirador.programacion.models.vo.Parada;
import pe.edu.unc.elmirador.programacion.models.vo.Ruta;
import pe.edu.unc.elmirador.programacion.models.vo.TipoDeCarga;
import pe.edu.unc.elmirador.programacion.models.vo.VentanaDeTiempo;

/**
 * Slice S1b: consolidacion y estiba. VIA-02 a VIA-06.
 *
 * <p>Es la regla que mas valor agrega del sistema: decidir que ordenes viajan juntas. Cuatro
 * invariantes se evaluan en la misma operacion, asi que tambien se prueba que un rechazo no deje
 * el viaje a medias.
 */
class ConsolidacionTest {

    private static final ZoneOffset LIMA = ZoneOffset.ofHours(-5);
    private static final Capacidad CAMION = new Capacidad(10_000, new BigDecimal("32.00"));
    private static final ClausulaDeConsolidacion PERMITIDA = ClausulaDeConsolidacion.consolidacionPermitida();

    private final Ruta corredorNorte = new Ruta("CAJAMARCA", "TRUJILLO", "NORTE");
    private final Ruta corredorSur = new Ruta("CAJAMARCA", "LIMA", "SUR");

    private VentanaDeTiempo ventana(int diaDesde, int diaHasta) {
        return new VentanaDeTiempo(
                OffsetDateTime.of(2026, 9, diaDesde, 6, 0, 0, 0, LIMA),
                OffsetDateTime.of(2026, 9, diaHasta, 18, 0, 0, 0, LIMA));
    }

    private Carga carga(String orden, int pesoKg, String volumen, TipoDeCarga tipo, int secuencia) {
        return new Carga(orden, pesoKg, new BigDecimal(volumen), tipo, secuencia);
    }

    private Viaje viajeCon(Carga inicial) {
        return Viaje.planificar("VIA-001", corredorNorte, ventana(10, 10), inicial);
    }

    // =========================================================================
    // VIA-02 — capacidad de la unidad
    // =========================================================================

    @Test
    @DisplayName("VIA-02: consolidar una carga que excede el peso de la unidad lanza")
    void excederElPesoLanza_VIA02() {
        Viaje viaje = viajeCon(carga("ORD-1", 6_000, "10.00", TipoDeCarga.PALETIZADA, 1));

        assertThatThrownBy(() -> viaje.consolidarOrden(
                        carga("ORD-2", 4_001, "10.00", TipoDeCarga.PALETIZADA, 2),
                        corredorNorte, ventana(10, 10), PERMITIDA, CAMION))
                .isInstanceOf(CapacidadExcedidaException.class)
                .hasMessageContaining("VIA-02");
    }

    @Test
    @DisplayName("VIA-02: en el limite exacto de peso no lanza")
    void enElLimiteExactoDePesoNoLanza_VIA02() {
        Viaje viaje = viajeCon(carga("ORD-1", 6_000, "10.00", TipoDeCarga.PALETIZADA, 1));

        assertThatCode(() -> viaje.consolidarOrden(
                        carga("ORD-2", 4_000, "10.00", TipoDeCarga.PALETIZADA, 2),
                        corredorNorte, ventana(10, 10), PERMITIDA, CAMION))
                .doesNotThrowAnyException();
        assertThat(viaje.cargaConsolidada().pesoTotal()).isEqualTo(10_000);
    }

    @Test
    @DisplayName("VIA-02: el volumen se comprueba aparte del peso, y tambien en su limite exacto")
    void elVolumenSeCompruebaAparte_VIA02() {
        Viaje viaje = viajeCon(carga("ORD-1", 1_000, "20.00", TipoDeCarga.PALETIZADA, 1));

        // Cabe de sobra en peso, pero se pasa un centesimo de metro cubico.
        assertThatThrownBy(() -> viaje.consolidarOrden(
                        carga("ORD-2", 1_000, "12.01", TipoDeCarga.PALETIZADA, 2),
                        corredorNorte, ventana(10, 10), PERMITIDA, CAMION))
                .isInstanceOf(CapacidadExcedidaException.class);

        assertThatCode(() -> viaje.consolidarOrden(
                        carga("ORD-3", 1_000, "12.00", TipoDeCarga.PALETIZADA, 2),
                        corredorNorte, ventana(10, 10), PERMITIDA, CAMION))
                .doesNotThrowAnyException();
    }

    // =========================================================================
    // VIA-03 — mismo corredor y ventanas compatibles
    // =========================================================================

    @Test
    @DisplayName("VIA-03: una orden de otro corredor no se consolida")
    void otroCorredorLanza_VIA03() {
        Viaje viaje = viajeCon(carga("ORD-1", 1_000, "5.00", TipoDeCarga.GENERAL, 1));

        assertThatThrownBy(() -> viaje.consolidarOrden(
                        carga("ORD-2", 1_000, "5.00", TipoDeCarga.GENERAL, 2),
                        corredorSur, ventana(10, 10), PERMITIDA, CAMION))
                .isInstanceOf(CorredorIncompatibleException.class)
                .hasMessageContaining("VIA-03");
    }

    @Test
    @DisplayName("VIA-03: mismo corredor pero ventanas disjuntas tampoco se consolida")
    void ventanasDisjuntasLanza_VIA03() {
        Viaje viaje = viajeCon(carga("ORD-1", 1_000, "5.00", TipoDeCarga.GENERAL, 1));

        assertThatThrownBy(() -> viaje.consolidarOrden(
                        carga("ORD-2", 1_000, "5.00", TipoDeCarga.GENERAL, 2),
                        corredorNorte, ventana(20, 21), PERMITIDA, CAMION))
                .isInstanceOf(CorredorIncompatibleException.class)
                .hasMessageContaining("ventana");
    }

    // =========================================================================
    // VIA-04 — la clausula del contrato marco manda
    // =========================================================================

    @Test
    @DisplayName("VIA-04: si el contrato marco lo prohibe, no se consolida aunque todo lo demas cuadre")
    void clausulaProhibidaLanza_VIA04() {
        Viaje viaje = viajeCon(carga("ORD-1", 1_000, "5.00", TipoDeCarga.GENERAL, 1));
        ClausulaDeConsolidacion prohibida =
                ClausulaDeConsolidacion.consolidacionProhibida(List.of("CLIENTE_EXCLUSIVO"));

        assertThatThrownBy(() -> viaje.consolidarOrden(
                        carga("ORD-2", 1_000, "5.00", TipoDeCarga.GENERAL, 2),
                        corredorNorte, ventana(10, 10), prohibida, CAMION))
                .isInstanceOf(ConsolidacionProhibidaException.class)
                .hasMessageContaining("VIA-04");
    }

    // =========================================================================
    // VIA-05 — compatibilidad fisica, contra TODAS las cargas ya consolidadas
    // =========================================================================

    @Test
    @DisplayName("VIA-05: maquinaria pesada no comparte viaje con carga paletizada")
    void cargasIncompatiblesLanzan_VIA05() {
        Viaje viaje = viajeCon(carga("ORD-1", 1_000, "5.00", TipoDeCarga.PALETIZADA, 1));

        assertThatThrownBy(() -> viaje.consolidarOrden(
                        carga("ORD-2", 1_000, "5.00", TipoDeCarga.MAQUINARIA_PESADA, 2),
                        corredorNorte, ventana(10, 10), PERMITIDA, CAMION))
                .isInstanceOf(CargaIncompatibleException.class)
                .hasMessageContaining("VIA-05");
    }

    @Test
    @DisplayName("VIA-05: la tercera carga se compara contra la PRIMERA, no solo contra la ultima")
    void seComparaContraTodasLasCargas_VIA05() {
        Viaje viaje = viajeCon(carga("ORD-1", 1_000, "5.00", TipoDeCarga.MAQUINARIA_PESADA, 1));
        viaje.consolidarOrden(carga("ORD-2", 1_000, "5.00", TipoDeCarga.MAQUINARIA_PESADA, 2),
                corredorNorte, ventana(10, 10), PERMITIDA, CAMION);

        // GENERAL es compatible con GENERAL, pero aqui no hay ninguna GENERAL previa:
        // hay dos MAQUINARIA_PESADA, y con la primera ya choca.
        assertThatThrownBy(() -> viaje.consolidarOrden(
                        carga("ORD-3", 1_000, "5.00", TipoDeCarga.GENERAL, 3),
                        corredorNorte, ventana(10, 10), PERMITIDA, CAMION))
                .isInstanceOf(CargaIncompatibleException.class)
                .hasMessageContaining("ORD-1");
    }

    @Test
    @DisplayName("VIA-05: la matriz de compatibilidad es simetrica en las nueve combinaciones")
    void matrizDeCompatibilidadSimetrica_VIA05() {
        for (TipoDeCarga a : TipoDeCarga.values()) {
            for (TipoDeCarga b : TipoDeCarga.values()) {
                boolean esperado = (a == TipoDeCarga.MAQUINARIA_PESADA || b == TipoDeCarga.MAQUINARIA_PESADA)
                        ? a == b
                        : true;
                assertThat(a.esCompatibleCon(b))
                        .as("[VIA-05] %s con %s", a, b)
                        .isEqualTo(esperado);
                assertThat(b.esCompatibleCon(a))
                        .as("[VIA-05] simetria: %s con %s", b, a)
                        .isEqualTo(esperado);
            }
        }
    }

    // =========================================================================
    // VIA-06 — secuencia de estiba
    // =========================================================================

    @Test
    @DisplayName("VIA-06: lo que se descarga primero se estiba al final")
    void secuenciaDeEstibaEsInversaALaDescarga_VIA06() {
        HojaDeRuta hoja = HojaDeRuta.de(
                Parada.de(1, Parada.CARGA, "ORD-1"),
                Parada.de(2, Parada.DESCARGA, "ORD-1"),
                Parada.de(3, Parada.DESCARGA, "ORD-2"),
                Parada.de(4, Parada.DESCARGA, "ORD-3"));

        List<Parada> estiba = hoja.secuenciaDeEstiba();

        assertThat(estiba).extracting(Parada::ordenDeServicioId)
                .as("[VIA-06] la orden que se descarga primero va encima, o sea se estiba al final")
                .containsExactly("ORD-3", "ORD-2", "ORD-1");
        assertThat(estiba)
                .as("[VIA-06] las paradas de carga no entran en la estiba")
                .hasSize(3);
    }

    // =========================================================================
    // D6 — una consolidacion rechazada no deja el viaje a medias
    // =========================================================================

    @Test
    @DisplayName("D6: una consolidacion rechazada no altera la carga ni la lista de ordenes")
    void elRechazoNoMutaNada() {
        Viaje viaje = viajeCon(carga("ORD-1", 6_000, "10.00", TipoDeCarga.PALETIZADA, 1));
        int pesoAntes = viaje.cargaConsolidada().pesoTotal();
        List<String> ordenesAntes = viaje.ordenIds();

        assertThatThrownBy(() -> viaje.consolidarOrden(
                        carga("ORD-2", 9_000, "10.00", TipoDeCarga.PALETIZADA, 2),
                        corredorNorte, ventana(10, 10), PERMITIDA, CAMION))
                .isInstanceOf(CapacidadExcedidaException.class);

        assertThat(viaje.cargaConsolidada().pesoTotal()).isEqualTo(pesoAntes);
        assertThat(viaje.ordenIds()).isEqualTo(ordenesAntes);
        assertThat(viaje.ordenIds()).doesNotContain("ORD-2");
    }

    @Test
    @DisplayName("una consolidacion aceptada registra la orden una sola vez")
    void laConsolidacionAceptadaRegistraLaOrden() {
        Viaje viaje = viajeCon(carga("ORD-1", 1_000, "5.00", TipoDeCarga.GENERAL, 1));

        viaje.consolidarOrden(carga("ORD-2", 1_000, "5.00", TipoDeCarga.GENERAL, 2),
                corredorNorte, ventana(10, 10), PERMITIDA, CAMION);

        assertThat(viaje.ordenIds()).containsExactly("ORD-1", "ORD-2");
        assertThat(viaje.cargaConsolidada().cargas()).hasSize(2);
    }
}
