package pe.edu.unc.elmirador.ejecucion.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.ejecucion.exceptions.CheckListNoAprobadoException;
import pe.edu.unc.elmirador.ejecucion.exceptions.EjecucionEntregadaException;
import pe.edu.unc.elmirador.ejecucion.exceptions.EvidenciaRequeridaException;
import pe.edu.unc.elmirador.ejecucion.exceptions.LiquidacionPendienteException;
import pe.edu.unc.elmirador.ejecucion.exceptions.TransicionDeEjecucionInvalidaException;
import pe.edu.unc.elmirador.ejecucion.models.entity.ConformidadDeEntrega;
import pe.edu.unc.elmirador.ejecucion.models.entity.EjecucionDeViaje;
import pe.edu.unc.elmirador.ejecucion.models.entity.Hito;
import pe.edu.unc.elmirador.ejecucion.models.entity.Incidencia;
import pe.edu.unc.elmirador.ejecucion.models.entity.Parada;
import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoConformidad;
import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoDeEjecucion;
import pe.edu.unc.elmirador.ejecucion.models.vo.Evidencia;
import pe.edu.unc.elmirador.ejecucion.models.vo.ResultadoDeCheckList;
import pe.edu.unc.elmirador.ejecucion.models.vo.TipoDeHito;
import pe.edu.unc.elmirador.ejecucion.models.vo.TipoDeIncidencia;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EjecucionDeViajeTest {

    private static final ZoneOffset LIMA = ZoneOffset.ofHours(-5);
    private static final OffsetDateTime T08_00 = OffsetDateTime.of(2026, 9, 10, 8, 0, 0, 0, LIMA);
    private static final OffsetDateTime T12_00 = OffsetDateTime.of(2026, 9, 10, 12, 0, 0, 0, LIMA);
    private static final OffsetDateTime T16_00 = OffsetDateTime.of(2026, 9, 10, 16, 0, 0, 0, LIMA);

    private EjecucionDeViaje crearEjecucionBasica() {
        Parada parada = new Parada(1, "ORD-001", "Av. Industrial 123");
        return EjecucionDeViaje.crear("VIA-001", "UNI-001", List.of("CON-001"), List.of(parada));
    }

    private EjecucionDeViaje crearEjecucionEntregada() {
        EjecucionDeViaje ejecucion = crearEjecucionBasica();
        ejecucion.registrarCheckList(ResultadoDeCheckList.aprobado(T08_00));
        ejecucion.iniciar(T08_00);

        ConformidadDeEntrega conformidad = new ConformidadDeEntrega(
                "CONF-001",
                "ORD-001",
                EstadoConformidad.FIRMADA,
                "Juan Perez",
                T12_00,
                "Conforme"
        );
        ejecucion.registrarConformidad(1, conformidad);
        ejecucion.marcarEntregada(T12_00);
        return ejecucion;
    }

    // ==========================================
    // EJV-01: Iniciar viaje y check-list
    // ==========================================

    @Test
    @DisplayName("[EJV-01] iniciar sin check-list registrado lanza CheckListNoAprobadoException")
    void iniciarSinCheckListLanzaExcepcion_EJV01() {
        EjecucionDeViaje ejecucion = crearEjecucionBasica();

        assertThatThrownBy(() -> ejecucion.iniciar(T08_00))
                .as("[EJV-01] Iniciar sin check-list registrado debe fallar")
                .isInstanceOf(CheckListNoAprobadoException.class)
                .hasMessageContaining("sin check-list");

        assertThat(ejecucion.getEstado()).isEqualTo(EstadoDeEjecucion.PENDIENTE);
    }

    @Test
    @DisplayName("[EJV-01] iniciar con check-list NO aprobado lanza CheckListNoAprobadoException")
    void iniciarConCheckListNoAprobadoLanzaExcepcion_EJV01() {
        EjecucionDeViaje ejecucion = crearEjecucionBasica();
        ResultadoDeCheckList noAprobado = ResultadoDeCheckList.noAprobado(
                List.of("Fuga de aceite en motor"),
                T08_00
        );
        ejecucion.registrarCheckList(noAprobado);

        assertThatThrownBy(() -> ejecucion.iniciar(T08_00))
                .as("[EJV-01] Iniciar con check-list no aprobado debe fallar")
                .isInstanceOf(CheckListNoAprobadoException.class)
                .hasMessageContaining("sin check-list de salida aprobado");

        assertThat(ejecucion.getEstado()).isEqualTo(EstadoDeEjecucion.PENDIENTE);
    }

    @Test
    @DisplayName("[EJV-01] iniciar con check-list aprobado cambia estado a EN_RUTA")
    void iniciarConCheckListAprobadoPasaAEnRuta_EJV01() {
        EjecucionDeViaje ejecucion = crearEjecucionBasica();
        ejecucion.registrarCheckList(ResultadoDeCheckList.aprobado(T08_00));

        ejecucion.iniciar(T08_00);

        assertThat(ejecucion.getEstado()).isEqualTo(EstadoDeEjecucion.EN_RUTA);
        assertThat(ejecucion.getFechaInicio()).isEqualTo(T08_00);
    }

    // ==========================================
    // EJV-04: Inmutabilidad de ejecucion entregada
    // ==========================================

    @Test
    @DisplayName("[EJV-04] Sobre ejecucion ENTREGADA reportarHito lanza EjecucionEntregadaException")
    void reportarHitoSobreEjecucionEntregadaLanzaExcepcion_EJV04() {
        EjecucionDeViaje ejecucion = crearEjecucionEntregada();
        Hito hito = new Hito("HITO-01", TipoDeHito.PASO_DE_CONTROL, T16_00, "Peaje Chicama");

        assertThatThrownBy(() -> ejecucion.reportarHito(hito))
                .as("[EJV-04] No se admiten hitos sobre ejecucion entregada")
                .isInstanceOf(EjecucionEntregadaException.class)
                .hasMessageContaining("No se pueden reportar hitos");
    }

    @Test
    @DisplayName("[EJV-04] Sobre ejecucion ENTREGADA reabrir una parada ATENDIDA lanza EjecucionEntregadaException")
    void reabrirParadaAtendidaSobreEjecucionEntregadaLanzaExcepcion_EJV04() {
        EjecucionDeViaje ejecucion = crearEjecucionEntregada();

        assertThatThrownBy(() -> ejecucion.reabrirParada(1))
                .as("[EJV-04] No se admite reapertura de paradas atendidas sobre ejecucion entregada")
                .isInstanceOf(EjecucionEntregadaException.class)
                .hasMessageContaining("reapertura de paradas atendidas");
    }

    @Test
    @DisplayName("[EJV-04] Sobre ejecucion CERRADA reportarHito lanza EjecucionEntregadaException")
    void reportarHitoSobreEjecucionCerradaLanzaExcepcion_EJV04() {
        EjecucionDeViaje ejecucion = crearEjecucionEntregada();
        ejecucion.cerrar(184320, false, Set.of("CON-001"), Set.of());

        Hito hito = new Hito("HITO-02", TipoDeHito.LLEGADA_A_DESTINO, T16_00, "Destino Final");

        assertThatThrownBy(() -> ejecucion.reportarHito(hito))
                .as("[EJV-04] No se admiten hitos sobre ejecucion cerrada")
                .isInstanceOf(EjecucionEntregadaException.class);
    }

    // ==========================================
    // LIQ-04: Cierre de ejecucion y liquidaciones
    // ==========================================

    @Test
    @DisplayName("[LIQ-04] cerrar con liquidaciones pendientes (true) lanza LiquidacionPendienteException")
    void cerrarConLiquidacionesPendientesLanzaExcepcion_LIQ04() {
        EjecucionDeViaje ejecucion = crearEjecucionEntregada();

        assertThatThrownBy(() -> ejecucion.cerrar(184320, true, Set.of("CON-001"), Set.of()))
                .as("[LIQ-04] No se puede cerrar la ejecucion con liquidaciones pendientes")
                .isInstanceOf(LiquidacionPendienteException.class)
                .hasMessageContaining("liquidaciones pendientes");

        assertThat(ejecucion.getEstado()).isEqualTo(EstadoDeEjecucion.ENTREGADA);
    }

    @Test
    @DisplayName("[LIQ-04] cerrar sin liquidaciones pendientes (false) sobre ejecucion ENTREGADA pasa a CERRADA")
    void cerrarSinLiquidacionesPendientesPasaACerrada_LIQ04() {
        EjecucionDeViaje ejecucion = crearEjecucionEntregada();

        ejecucion.cerrar(184320, false, Set.of("CON-001"), Set.of());

        assertThat(ejecucion.getEstado()).isEqualTo(EstadoDeEjecucion.CERRADA);
    }

    @Test
    @DisplayName("cerrar sobre ejecucion no ENTREGADA lanza TransicionDeEjecucionInvalidaException")
    void cerrarSobreEjecucionEnRutaLanzaExcepcion() {
        EjecucionDeViaje ejecucion = crearEjecucionBasica();
        ejecucion.registrarCheckList(ResultadoDeCheckList.aprobado(T08_00));
        ejecucion.iniciar(T08_00);

        assertThatThrownBy(() -> ejecucion.cerrar(184320, false, Set.of("CON-001"), Set.of()))
                .isInstanceOf(TransicionDeEjecucionInvalidaException.class);
    }

    // ==========================================
    // Incidencias con evidencia obligatoria
    // ==========================================

    @Test
    @DisplayName("Incidencia de DANIO sin evidencia lanza EvidenciaRequeridaException")
    void incidenciaDanioSinEvidenciaLanzaExcepcion() {
        assertThatThrownBy(() -> new Incidencia("INC-01", TipoDeIncidencia.DANIO, "Caja golpeada", null, T12_00))
                .isInstanceOf(EvidenciaRequeridaException.class)
                .hasMessageContaining("exige evidencia obligatoria");
    }

    @Test
    @DisplayName("Incidencia de FALTANTE sin evidencia lanza EvidenciaRequeridaException")
    void incidenciaFaltanteSinEvidenciaLanzaExcepcion() {
        assertThatThrownBy(() -> new Incidencia("INC-02", TipoDeIncidencia.FALTANTE, "Falta 1 bulto", null, T12_00))
                .isInstanceOf(EvidenciaRequeridaException.class)
                .hasMessageContaining("exige evidencia obligatoria");
    }

    @Test
    @DisplayName("Incidencia de RECHAZO_DE_CARGA sin evidencia lanza EvidenciaRequeridaException")
    void incidenciaRechazoSinEvidenciaLanzaExcepcion() {
        assertThatThrownBy(() -> new Incidencia("INC-03", TipoDeIncidencia.RECHAZO_DE_CARGA, "Rechazo por fecha", null, T12_00))
                .isInstanceOf(EvidenciaRequeridaException.class)
                .hasMessageContaining("exige evidencia obligatoria");
    }

    @Test
    @DisplayName("Incidencia de DEMORA sin evidencia se registra correctamente sin lanzar")
    void incidenciaDemoraSinEvidenciaNoLanza() {
        EjecucionDeViaje ejecucion = crearEjecucionBasica();
        Incidencia demora = new Incidencia("INC-04", TipoDeIncidencia.DEMORA, "Trafico pesado", null, T12_00);

        ejecucion.registrarIncidencia(demora);

        assertThat(ejecucion.getIncidencias()).hasSize(1);
        assertThat(ejecucion.getIncidencias().get(0).getTipo()).isEqualTo(TipoDeIncidencia.DEMORA);
    }

    @Test
    @DisplayName("incidenciasSinResolver retorna solo las que exigen evidencia y no estan resueltas")
    void incidenciasSinResolverFiltraCorrectamente() {
        EjecucionDeViaje ejecucion = crearEjecucionBasica();
        Evidencia evidencia = new Evidencia(List.of("https://cdn.com/foto.jpg"), "Detalle danio", T12_00);

        Incidencia danioAbierto = new Incidencia("INC-01", TipoDeIncidencia.DANIO, "Danio 1", evidencia, T12_00);
        Incidencia danioResuelto = new Incidencia("INC-02", TipoDeIncidencia.DANIO, "Danio 2", evidencia, T12_00);
        danioResuelto.resolver();
        Incidencia demora = new Incidencia("INC-03", TipoDeIncidencia.DEMORA, "Demora en ruta", null, T12_00);

        ejecucion.registrarIncidencia(danioAbierto);
        ejecucion.registrarIncidencia(danioResuelto);
        ejecucion.registrarIncidencia(demora);

        List<Incidencia> sinResolver = ejecucion.incidenciasSinResolver();

        assertThat(sinResolver).containsExactly(danioAbierto);
    }

    // ==========================================
    // Bordes: Operaciones con fecha nula (D1)
    // ==========================================

    @Test
    @DisplayName("[D1] Toda operacion con fecha nula lanza IllegalArgumentException")
    void operacionesConFechaNulaLanzanExcepcion() {
        EjecucionDeViaje ejecucion = crearEjecucionBasica();
        ejecucion.registrarCheckList(ResultadoDeCheckList.aprobado(T08_00));

        assertThatThrownBy(() -> ejecucion.iniciar(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inicio");

        ejecucion.iniciar(T08_00);

        assertThatThrownBy(() -> ejecucion.marcarEntregada(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entrega");
    }
    @Test
    @DisplayName("dejaUnidadInoperativa: AVERIA no resuelta -> true; AVERIA resuelta -> false; DEMORA -> false")
    void dejaUnidadInoperativaTest() {
        Incidencia averiaNoResuelta = new Incidencia("1", TipoDeIncidencia.AVERIA, "desc", null, T12_00);
        assertThat(averiaNoResuelta.dejaUnidadInoperativa()).isTrue();
        
        Incidencia averiaResuelta = new Incidencia("2", TipoDeIncidencia.AVERIA, "desc", null, T12_00);
        averiaResuelta.resolver();
        assertThat(averiaResuelta.dejaUnidadInoperativa()).isFalse();
        
        Incidencia demora = new Incidencia("3", TipoDeIncidencia.DEMORA, "desc", null, T12_00);
        assertThat(demora.dejaUnidadInoperativa()).isFalse();
    }

    @Test
    @DisplayName("fallasDeUnidad y incidenciasImputablesAlConductor devuelven lo correspondiente")
    void fallasEIncidenciasTest() {
        EjecucionDeViaje ejecucion = crearEjecucionBasica();
        Evidencia ev = new Evidencia(List.of("f"), "d", T12_00);
        Incidencia averia = new Incidencia("1", TipoDeIncidencia.AVERIA, "d", null, T12_00);
        Incidencia danio = new Incidencia("2", TipoDeIncidencia.DANIO, "d", ev, T12_00);
        Incidencia faltante = new Incidencia("3", TipoDeIncidencia.FALTANTE, "d", ev, T12_00);
        Incidencia clima = new Incidencia("4", TipoDeIncidencia.CLIMA, "d", null, T12_00);
        
        ejecucion.registrarIncidencia(averia);
        ejecucion.registrarIncidencia(danio);
        ejecucion.registrarIncidencia(faltante);
        ejecucion.registrarIncidencia(clima);
        
        List<Incidencia> fallas = ejecucion.fallasDeUnidad();
        assertThat(fallas).containsExactly(averia);
        
        List<Incidencia> imputables = ejecucion.incidenciasImputablesAlConductor();
        assertThat(imputables).containsExactly(danio, faltante);
    }

    @Test
    @DisplayName("cerrar con kilometrajeFinal <= 0 lanza IllegalArgumentException")
    void cerrarKilometrajeInvalido() {
        EjecucionDeViaje ejecucion = crearEjecucionEntregada();
        assertThatThrownBy(() -> ejecucion.cerrar(0, false, Set.of("CON-001"), Set.of("ORD-001")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ejecucion.cerrar(-10, false, Set.of("CON-001"), Set.of("ORD-001")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("cerrar fija el kilometrajeFinal en el agregado")
    void cerrarFijaKilometraje() {
        EjecucionDeViaje ejecucion = crearEjecucionEntregada();
        ejecucion.cerrar(1500, false, Set.of("CON-001"), Set.of("ORD-001"));
        assertThat(ejecucion.getKilometrajeFinal()).isEqualTo(1500);
    }

    @Test
    @DisplayName("Constructor sin conductores lanza IllegalArgumentException")
    void constructorSinConductores() {
        List<Parada> paradas = List.of(new Parada(1, "ORD-001", "Dir"));
        assertThatThrownBy(() -> EjecucionDeViaje.crear("V-1", "U-1", List.of(), paradas))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EjecucionDeViaje.crear("V-1", "U-1", null, paradas))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("paradasConEspera y paradasAtendidas devuelven lo correspondiente")
    void paradasMetodos() {
        Parada p1 = new Parada(1, "ORD-001", "D1");
        Parada p2 = new Parada(2, "ORD-002", "D2");
        EjecucionDeViaje ejecucion = EjecucionDeViaje.crear("V-1", "U-1", List.of("C-1"), List.of(p1, p2));
        
        ejecucion.registrarCheckList(ResultadoDeCheckList.aprobado(T08_00));
        ejecucion.iniciar(T08_00);
        
        p1.registrarEsperaFacturable(new pe.edu.unc.elmirador.ejecucion.models.vo.EsperaFacturable(T08_00, T12_00, 2));
        p1.registrarConformidad(new ConformidadDeEntrega("C1", "ORD-001", EstadoConformidad.FIRMADA, "Juan", T12_00, ""));
        
        assertThat(ejecucion.paradasConEspera()).containsExactly(p1);
        assertThat(ejecucion.paradasAtendidas()).containsExactly(p1);
    }
}
