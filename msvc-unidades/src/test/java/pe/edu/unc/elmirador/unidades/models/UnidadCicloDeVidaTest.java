package pe.edu.unc.elmirador.unidades.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.unidades.exceptions.ReactivacionInvalidaException;
import pe.edu.unc.elmirador.unidades.models.entity.DocumentoVehicular;
import pe.edu.unc.elmirador.unidades.models.entity.Unidad;
import pe.edu.unc.elmirador.unidades.models.vo.Capacidad;
import pe.edu.unc.elmirador.unidades.models.vo.EstadoOperativo;
import pe.edu.unc.elmirador.unidades.models.vo.IntervaloDeMantenimiento;
import pe.edu.unc.elmirador.unidades.models.vo.Kilometraje;
import pe.edu.unc.elmirador.unidades.models.vo.PeriodoDeVigencia;
import pe.edu.unc.elmirador.unidades.models.vo.Placa;
import pe.edu.unc.elmirador.unidades.models.vo.ProgramaDeMantenimiento;
import pe.edu.unc.elmirador.unidades.models.vo.SituacionOperativa;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeCarga;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeDocumento;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeUnidad;

/**
 * Ciclo de vida del estado operativo: como se sale de servicio y como se vuelve.
 *
 * <p>Cubre los huecos que UnidadTest no cerraba: registrar un documento vencido, rehabilitacion
 * automatica, perdida del motivo no documental y lectura implicita del reloj. Sin estas pruebas,
 * una implementacion que devuelva una unidad a servicio al renovar un papel pasa el gate.
 */
class UnidadCicloDeVidaTest {

    private final LocalDate hoy = LocalDate.of(2026, 9, 4);

    private PeriodoDeVigencia vigente() {
        return new PeriodoDeVigencia(hoy.minusMonths(6), hoy.plusMonths(6));
    }

    private PeriodoDeVigencia vencida() {
        return new PeriodoDeVigencia(hoy.minusYears(2), hoy.minusDays(1));
    }

    private List<DocumentoVehicular> documentosVigentes() {
        List<DocumentoVehicular> docs = new ArrayList<>();
        for (TipoDeDocumento tipo : TipoDeDocumento.values()) {
            docs.add(new DocumentoVehicular("D-" + tipo.name(), tipo, vigente(), "N-" + tipo.name()));
        }
        return docs;
    }

    private Unidad unidad(List<DocumentoVehicular> documentos, Kilometraje km, ProgramaDeMantenimiento programa) {
        return new Unidad(
                "UNI-100",
                new Placa("XYZ-987"),
                TipoDeUnidad.PLATAFORMA,
                new Capacidad(20_000, new BigDecimal("40.00")),
                km,
                EstadoOperativo.operativa(),
                programa,
                documentos);
    }

    private ProgramaDeMantenimiento programaAlDia() {
        return new ProgramaDeMantenimiento(
                new Kilometraje(10_000), new Kilometraje(20_000), IntervaloDeMantenimiento.ACEITE_Y_FILTROS);
    }

    // =========================================================================
    // UNI-01 — registrar un documento ya vencido no puede devolver la unidad a servicio
    // =========================================================================

    @Test
    @DisplayName("UNI-01: registrar un documento YA VENCIDO deja la unidad INOPERATIVA")
    void registrarDocumentoYaVencidoNoRehabilita() {
        List<DocumentoVehicular> docs = documentosVigentes();
        docs.removeIf(d -> d.getTipo() == TipoDeDocumento.SOAT);
        Unidad unidad = unidad(docs, new Kilometraje(12_000), programaAlDia());
        unidad.evaluarVigenciaDocumental(hoy);
        assertThat(unidad.getEstadoOperativo().situacion()).isEqualTo(SituacionOperativa.INOPERATIVA);

        // Se registra un SOAT que expiro ayer. Evaluar en su fecha de inicio lo daria por vigente.
        unidad.registrarDocumento(TipoDeDocumento.SOAT, vencida(), "SOAT-CADUCO", hoy);

        assertThat(unidad.getEstadoOperativo().situacion())
                .as("UNI-01: un documento vencido no habilita, sea cual sea su fecha de inicio")
                .isEqualTo(SituacionOperativa.INOPERATIVA);
        assertThat(unidad.estaHabilitada(hoy)).isFalse();
    }

    @Test
    @DisplayName("UNI-01: renovar todos los documentos NO devuelve sola la unidad a servicio")
    void renovarDocumentosNoRehabilitaAutomaticamente() {
        List<DocumentoVehicular> docs = documentosVigentes();
        docs.removeIf(d -> d.getTipo() == TipoDeDocumento.PERMISO_MTC);
        Unidad unidad = unidad(docs, new Kilometraje(12_000), programaAlDia());
        unidad.evaluarVigenciaDocumental(hoy);
        assertThat(unidad.getEstadoOperativo().situacion()).isEqualTo(SituacionOperativa.INOPERATIVA);

        unidad.registrarDocumento(TipoDeDocumento.PERMISO_MTC, vigente(), "MTC-NUEVO", hoy);

        assertThat(unidad.getEstadoOperativo().situacion())
                .as("volver a servicio es un acto deliberado, no un efecto colateral de renovar un papel")
                .isEqualTo(SituacionOperativa.INOPERATIVA);
    }

    @Test
    @DisplayName("evaluarVigenciaDocumental no pisa un motivo no documental ya vigente")
    void noPierdePorQueEstabaParada() {
        Unidad unidad = unidad(documentosVigentes(), new Kilometraje(12_000), programaAlDia());
        unidad.marcarInoperativa("SINIESTRO_FRONTAL");

        List<DocumentoVehicular> sinSoat = new ArrayList<>(unidad.getDocumentos());
        sinSoat.removeIf(d -> d.getTipo() == TipoDeDocumento.SOAT);
        Unidad otra = unidad(sinSoat, new Kilometraje(12_000), programaAlDia());
        otra.marcarInoperativa("SINIESTRO_FRONTAL");
        otra.evaluarVigenciaDocumental(hoy);

        assertThat(otra.getEstadoOperativo().motivo())
                .as("el motivo fisico sobrevive al vencimiento documental")
                .isEqualTo("SINIESTRO_FRONTAL");
    }

    // =========================================================================
    // reactivar() — no se puede volver a servicio saltandose UNI-01 ni UNI-02
    // =========================================================================

    @Test
    @DisplayName("UNI-01: reactivar con un documento vencido lanza ReactivacionInvalidaException")
    void reactivarConDocumentoVencidoLanza() {
        List<DocumentoVehicular> docs = documentosVigentes();
        docs.removeIf(d -> d.getTipo() == TipoDeDocumento.REVISION_TECNICA);
        docs.add(new DocumentoVehicular("D-RT", TipoDeDocumento.REVISION_TECNICA, vencida(), "RT-1"));
        Unidad unidad = unidad(docs, new Kilometraje(12_000), programaAlDia());
        unidad.marcarInoperativa("REVISION_PENDIENTE");

        assertThatThrownBy(() -> unidad.reactivar(hoy))
                .isInstanceOf(ReactivacionInvalidaException.class)
                .hasMessageContaining("UNI-01");
    }

    @Test
    @DisplayName("UNI-02: reactivar con mantenimiento preventivo vencido lanza")
    void reactivarConMantenimientoVencidoLanza() {
        Unidad unidad = unidad(documentosVigentes(), new Kilometraje(21_000), programaAlDia());
        unidad.marcarEnTaller("REVISION_MAYOR");

        assertThatThrownBy(() -> unidad.reactivar(hoy))
                .isInstanceOf(ReactivacionInvalidaException.class)
                .hasMessageContaining("UNI-02");
    }

    @Test
    @DisplayName("reactivar con documentos vigentes y mantenimiento al dia deja la unidad OPERATIVA")
    void reactivarConTodoEnRegla() {
        Unidad unidad = unidad(documentosVigentes(), new Kilometraje(12_000), programaAlDia());
        unidad.marcarEnTaller("CAMBIO_DE_LLANTAS");

        unidad.reactivar(hoy);

        assertThat(unidad.getEstadoOperativo().situacion()).isEqualTo(SituacionOperativa.OPERATIVA);
        assertThat(unidad.getEstadoOperativo().motivo()).isNull();
        assertThat(unidad.estaHabilitada(hoy)).isTrue();
    }

    @Test
    @DisplayName("UNI-02: cerrar el mantenimiento reprograma el proximo servicio y rehabilita")
    void registrarMantenimientoRealizadoReprograma() {
        Unidad unidad = unidad(documentosVigentes(), new Kilometraje(21_000), programaAlDia());
        assertThat(unidad.estaHabilitada(hoy)).isFalse();

        unidad.registrarMantenimientoRealizado(new Kilometraje(21_000));

        assertThat(unidad.getProgramaDeMantenimiento().kmProximoServicio().valor()).isEqualTo(31_000);
        assertThat(unidad.estaHabilitada(hoy)).isTrue();
    }

    // =========================================================================
    // El dominio no lee el reloj del sistema
    // =========================================================================

    @Test
    @DisplayName("toda operacion que depende de 'hoy' exige la fecha; el dominio no llama a LocalDate.now()")
    void elDominioNoLeeElReloj() {
        Unidad unidad = unidad(documentosVigentes(), new Kilometraje(12_000), programaAlDia());

        assertThatThrownBy(() -> unidad.estaHabilitada(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> unidad.evaluarVigenciaDocumental(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> unidad.reactivar(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () -> unidad.motivosDeNoElegibilidad(null, 1_000, new BigDecimal("2.0"), TipoDeCarga.GENERAL))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("UNI-02: una unidad sin programa de mantenimiento no se puede construir")
    void sinProgramaDeMantenimientoNoHayUnidad() {
        assertThatThrownBy(() -> unidad(documentosVigentes(), new Kilometraje(12_000), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UNI-02");
    }
}
