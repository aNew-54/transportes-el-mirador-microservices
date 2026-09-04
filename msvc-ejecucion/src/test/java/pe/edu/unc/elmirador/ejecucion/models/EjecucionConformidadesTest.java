package pe.edu.unc.elmirador.ejecucion.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.ejecucion.exceptions.ConformidadesPendientesException;
import pe.edu.unc.elmirador.ejecucion.exceptions.DominioEjecucionException;
import pe.edu.unc.elmirador.ejecucion.models.entity.ConformidadDeEntrega;
import pe.edu.unc.elmirador.ejecucion.models.entity.EjecucionDeViaje;
import pe.edu.unc.elmirador.ejecucion.models.entity.Parada;
import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoConformidad;
import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoDeEjecucion;
import pe.edu.unc.elmirador.ejecucion.models.vo.ResultadoDeCheckList;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EjecucionConformidadesTest {

    private static final ZoneOffset LIMA = ZoneOffset.ofHours(-5);
    private static final OffsetDateTime T08_00 = OffsetDateTime.of(2026, 9, 10, 8, 0, 0, 0, LIMA);
    private static final OffsetDateTime T14_00 = OffsetDateTime.of(2026, 9, 10, 14, 0, 0, 0, LIMA);

    private EjecucionDeViaje ejecucion;
    private Parada parada1;
    private Parada parada2;
    private Parada parada3;

    @BeforeEach
    void setUp() {
        parada1 = new Parada(1, "ORD-101", "Almacen Central Lima");
        parada2 = new Parada(2, "ORD-102", "Sucursal Huacho");
        parada3 = new Parada(3, "ORD-103", "Planta Barranca");

        ejecucion = EjecucionDeViaje.crear("VIA-999", "UNI-555", List.of(parada1, parada2, parada3));
        ejecucion.registrarCheckList(ResultadoDeCheckList.aprobado(T08_00));
        ejecucion.iniciar(T08_00);
    }

    // ==========================================
    // EJV-02: Conformidad por parada y orden
    // ==========================================

    @Test
    @DisplayName("[EJV-02] Viaje con tres paradas registra una conformidad por cada orden exitosamente")
    void registrarTresConformidadesUnaPorParada_EJV02() {
        ConformidadDeEntrega conf1 = new ConformidadDeEntrega("CONF-01", "ORD-101", EstadoConformidad.FIRMADA, "Receptor 1", T14_00, "OK");
        ConformidadDeEntrega conf2 = new ConformidadDeEntrega("CONF-02", "ORD-102", EstadoConformidad.FIRMADA, "Receptor 2", T14_00, "OK");
        ConformidadDeEntrega conf3 = new ConformidadDeEntrega("CONF-03", "ORD-103", EstadoConformidad.FIRMADA, "Receptor 3", T14_00, "OK");

        ejecucion.registrarConformidad(1, conf1);
        ejecucion.registrarConformidad(2, conf2);
        ejecucion.registrarConformidad(3, conf3);

        assertThat(parada1.getConformidad()).isEqualTo(conf1);
        assertThat(parada2.getConformidad()).isEqualTo(conf2);
        assertThat(parada3.getConformidad()).isEqualTo(conf3);
    }

    @Test
    @DisplayName("[EJV-02] Registrar dos conformidades en la misma parada lanza DominioEjecucionException")
    void dosConformidadesEnMismaParadaLanzaExcepcion_EJV02() {
        ConformidadDeEntrega conf1 = new ConformidadDeEntrega("CONF-01", "ORD-101", EstadoConformidad.FIRMADA, "Receptor 1", T14_00, "OK");
        ConformidadDeEntrega confDuplicada = new ConformidadDeEntrega("CONF-01B", "ORD-101", EstadoConformidad.FIRMADA, "Receptor 1", T14_00, "Duplicada");

        ejecucion.registrarConformidad(1, conf1);

        assertThatThrownBy(() -> ejecucion.registrarConformidad(1, confDuplicada))
                .as("[EJV-02] Registrar dos conformidades en la misma parada debe fallar")
                .isInstanceOf(DominioEjecucionException.class)
                .hasMessageContaining("ya cuenta con una conformidad");
    }

    @Test
    @DisplayName("[EJV-02] Registrar conformidad con orden de servicio distinta a la parada lanza DominioEjecucionException")
    void conformidadConOrdenDistintaLanzaExcepcion_EJV02() {
        ConformidadDeEntrega confErronea = new ConformidadDeEntrega("CONF-99", "ORD-INCORRECTA", EstadoConformidad.FIRMADA, "Receptor X", T14_00, "Err");

        assertThatThrownBy(() -> ejecucion.registrarConformidad(1, confErronea))
                .as("[EJV-02] Conformidad con orden de servicio discordante debe ser rechazada")
                .isInstanceOf(DominioEjecucionException.class)
                .hasMessageContaining("no coincide con la de la parada");
    }

    // ==========================================
    // EJV-03: Paso a ENTREGADA exige todas firmadas
    // ==========================================

    @Test
    @DisplayName("[EJV-03] Con dos de tres conformidades firmadas, marcarEntregada lanza y estado sigue EN_RUTA (D6)")
    void dosDeTresConformidadesLanzaYEstadoSigueEnRuta_EJV03() {
        ConformidadDeEntrega conf1 = new ConformidadDeEntrega("CONF-01", "ORD-101", EstadoConformidad.FIRMADA, "Receptor 1", T14_00, "OK");
        ConformidadDeEntrega conf2 = new ConformidadDeEntrega("CONF-02", "ORD-102", EstadoConformidad.FIRMADA, "Receptor 2", T14_00, "OK");

        ejecucion.registrarConformidad(1, conf1);
        ejecucion.registrarConformidad(2, conf2);

        assertThatThrownBy(() -> ejecucion.marcarEntregada(T14_00))
                .as("[EJV-03] Faltando una conformidad, marcarEntregada debe lanzar ConformidadesPendientesException")
                .isInstanceOf(ConformidadesPendientesException.class)
                .hasMessageContaining("sin conformidad firmada");

        // D6: Se valida todo antes de mutar nada: el estado debe seguir EN_RUTA
        assertThat(ejecucion.getEstado())
                .as("[D6] El estado debe seguir EN_RUTA tras fallar la validacion de conformidades")
                .isEqualTo(EstadoDeEjecucion.EN_RUTA);
    }

    @Test
    @DisplayName("[EJV-03] Con todas las conformidades firmadas, marcarEntregada pasa a ENTREGADA")
    void todasLasConformidadesFirmadasPasaAEntregada_EJV03() {
        ConformidadDeEntrega conf1 = new ConformidadDeEntrega("CONF-01", "ORD-101", EstadoConformidad.FIRMADA, "Receptor 1", T14_00, "OK");
        ConformidadDeEntrega conf2 = new ConformidadDeEntrega("CONF-02", "ORD-102", EstadoConformidad.FIRMADA, "Receptor 2", T14_00, "OK");
        ConformidadDeEntrega conf3 = new ConformidadDeEntrega("CONF-03", "ORD-103", EstadoConformidad.FIRMADA, "Receptor 3", T14_00, "OK");

        ejecucion.registrarConformidad(1, conf1);
        ejecucion.registrarConformidad(2, conf2);
        ejecucion.registrarConformidad(3, conf3);

        ejecucion.marcarEntregada(T14_00);

        assertThat(ejecucion.getEstado()).isEqualTo(EstadoDeEjecucion.ENTREGADA);
        assertThat(ejecucion.getFechaEntrega()).isEqualTo(T14_00);
    }

    @Test
    @DisplayName("[EJV-03] Si una conformidad esta OBSERVADA en lugar de FIRMADA, marcarEntregada lanza y sigue EN_RUTA")
    void conformidadObservadaImpideMarcarEntregada_EJV03() {
        ConformidadDeEntrega conf1 = new ConformidadDeEntrega("CONF-01", "ORD-101", EstadoConformidad.FIRMADA, "Receptor 1", T14_00, "OK");
        ConformidadDeEntrega conf2 = new ConformidadDeEntrega("CONF-02", "ORD-102", EstadoConformidad.OBSERVADA, "Receptor 2", T14_00, "Observado");
        ConformidadDeEntrega conf3 = new ConformidadDeEntrega("CONF-03", "ORD-103", EstadoConformidad.FIRMADA, "Receptor 3", T14_00, "OK");

        ejecucion.registrarConformidad(1, conf1);
        ejecucion.registrarConformidad(2, conf2);
        ejecucion.registrarConformidad(3, conf3);

        assertThatThrownBy(() -> ejecucion.marcarEntregada(T14_00))
                .isInstanceOf(ConformidadesPendientesException.class);

        assertThat(ejecucion.getEstado()).isEqualTo(EstadoDeEjecucion.EN_RUTA);
    }
}
