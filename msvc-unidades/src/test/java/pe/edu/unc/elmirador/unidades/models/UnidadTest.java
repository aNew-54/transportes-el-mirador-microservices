package pe.edu.unc.elmirador.unidades.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.unc.elmirador.unidades.exceptions.KilometrajeRetrocedeException;
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

class UnidadTest {

    private final LocalDate hoy = LocalDate.of(2026, 9, 4);

    private PeriodoDeVigencia vigenciaValida() {
        return new PeriodoDeVigencia(hoy.minusMonths(6), hoy.plusMonths(6));
    }

    private PeriodoDeVigencia vigenciaVencida() {
        return new PeriodoDeVigencia(hoy.minusYears(1), hoy.minusDays(1));
    }

    private List<DocumentoVehicular> crearCuatroDocumentosVigentes(String unidadId) {
        List<DocumentoVehicular> docs = new ArrayList<>();
        for (TipoDeDocumento tipo : TipoDeDocumento.values()) {
            docs.add(new DocumentoVehicular(
                    unidadId + "-" + tipo.name(),
                    tipo,
                    vigenciaValida(),
                    "DOC-" + tipo.name()));
        }
        return docs;
    }

    private Unidad crearUnidadOperativa(String id, List<DocumentoVehicular> documentos) {
        return new Unidad(
                id,
                new Placa("ABC-123"),
                TipoDeUnidad.FURGON,
                new Capacidad(10_000, new BigDecimal("32.00")),
                new Kilometraje(10_000),
                EstadoOperativo.operativa(),
                new ProgramaDeMantenimiento(
                        new Kilometraje(10_000),
                        new Kilometraje(20_000),
                        IntervaloDeMantenimiento.ACEITE_Y_FILTROS),
                documentos);
    }

    // =========================================================================
    // INVARIANTE UNI-01
    // "Una unidad con cualquiera de sus cuatro documentos vencido pasa
    //  automaticamente a inoperativa."
    // Cuatro casos (uno por tipo) + un caso con documento ausente.
    // =========================================================================

    @Test
    @DisplayName("UNI-01: Revision tecnica vencida pasa la unidad a INOPERATIVA con motivo DOCUMENTO_VENCIDO:REVISION_TECNICA")
    void revisionTecnicaVencidaPasaUnidadAInoperativa_UNI01() {
        List<DocumentoVehicular> docs = new ArrayList<>();
        docs.add(new DocumentoVehicular("D1", TipoDeDocumento.REVISION_TECNICA, vigenciaVencida(), "REV-01"));
        docs.add(new DocumentoVehicular("D2", TipoDeDocumento.SOAT, vigenciaValida(), "SOAT-01"));
        docs.add(new DocumentoVehicular("D3", TipoDeDocumento.PERMISO_MTC, vigenciaValida(), "MTC-01"));
        docs.add(new DocumentoVehicular("D4", TipoDeDocumento.HABILITACION_VEHICULAR, vigenciaValida(), "HAB-01"));

        Unidad unidad = crearUnidadOperativa("UNI-001", docs);
        unidad.evaluarVigenciaDocumental(hoy);

        // Invariante UNI-01
        assertThat(unidad.getEstadoOperativo().situacion()).isEqualTo(SituacionOperativa.INOPERATIVA);
        assertThat(unidad.getEstadoOperativo().motivo()).isEqualTo("DOCUMENTO_VENCIDO:REVISION_TECNICA");
    }

    @Test
    @DisplayName("UNI-01: SOAT vencido pasa la unidad a INOPERATIVA con motivo DOCUMENTO_VENCIDO:SOAT")
    void soatVencidoPasaUnidadAInoperativa_UNI01() {
        List<DocumentoVehicular> docs = new ArrayList<>();
        docs.add(new DocumentoVehicular("D1", TipoDeDocumento.REVISION_TECNICA, vigenciaValida(), "REV-01"));
        docs.add(new DocumentoVehicular("D2", TipoDeDocumento.SOAT, vigenciaVencida(), "SOAT-01"));
        docs.add(new DocumentoVehicular("D3", TipoDeDocumento.PERMISO_MTC, vigenciaValida(), "MTC-01"));
        docs.add(new DocumentoVehicular("D4", TipoDeDocumento.HABILITACION_VEHICULAR, vigenciaValida(), "HAB-01"));

        Unidad unidad = crearUnidadOperativa("UNI-001", docs);
        unidad.evaluarVigenciaDocumental(hoy);

        // Invariante UNI-01
        assertThat(unidad.getEstadoOperativo().situacion()).isEqualTo(SituacionOperativa.INOPERATIVA);
        assertThat(unidad.getEstadoOperativo().motivo()).isEqualTo("DOCUMENTO_VENCIDO:SOAT");
    }

    @Test
    @DisplayName("UNI-01: Permiso MTC vencido pasa la unidad a INOPERATIVA con motivo DOCUMENTO_VENCIDO:PERMISO_MTC")
    void permisoMtcVencidoPasaUnidadAInoperativa_UNI01() {
        List<DocumentoVehicular> docs = new ArrayList<>();
        docs.add(new DocumentoVehicular("D1", TipoDeDocumento.REVISION_TECNICA, vigenciaValida(), "REV-01"));
        docs.add(new DocumentoVehicular("D2", TipoDeDocumento.SOAT, vigenciaValida(), "SOAT-01"));
        docs.add(new DocumentoVehicular("D3", TipoDeDocumento.PERMISO_MTC, vigenciaVencida(), "MTC-01"));
        docs.add(new DocumentoVehicular("D4", TipoDeDocumento.HABILITACION_VEHICULAR, vigenciaValida(), "HAB-01"));

        Unidad unidad = crearUnidadOperativa("UNI-001", docs);
        unidad.evaluarVigenciaDocumental(hoy);

        // Invariante UNI-01
        assertThat(unidad.getEstadoOperativo().situacion()).isEqualTo(SituacionOperativa.INOPERATIVA);
        assertThat(unidad.getEstadoOperativo().motivo()).isEqualTo("DOCUMENTO_VENCIDO:PERMISO_MTC");
    }

    @Test
    @DisplayName("UNI-01: Habilitacion vehicular vencida pasa la unidad a INOPERATIVA con motivo DOCUMENTO_VENCIDO:HABILITACION_VEHICULAR")
    void habilitacionVehicularVencidaPasaUnidadAInoperativa_UNI01() {
        List<DocumentoVehicular> docs = new ArrayList<>();
        docs.add(new DocumentoVehicular("D1", TipoDeDocumento.REVISION_TECNICA, vigenciaValida(), "REV-01"));
        docs.add(new DocumentoVehicular("D2", TipoDeDocumento.SOAT, vigenciaValida(), "SOAT-01"));
        docs.add(new DocumentoVehicular("D3", TipoDeDocumento.PERMISO_MTC, vigenciaValida(), "MTC-01"));
        docs.add(new DocumentoVehicular("D4", TipoDeDocumento.HABILITACION_VEHICULAR, vigenciaVencida(), "HAB-01"));

        Unidad unidad = crearUnidadOperativa("UNI-001", docs);
        unidad.evaluarVigenciaDocumental(hoy);

        // Invariante UNI-01
        assertThat(unidad.getEstadoOperativo().situacion()).isEqualTo(SituacionOperativa.INOPERATIVA);
        assertThat(unidad.getEstadoOperativo().motivo()).isEqualTo("DOCUMENTO_VENCIDO:HABILITACION_VEHICULAR");
    }

    @Test
    @DisplayName("UNI-01: Documento faltante/ausente deja la unidad en INOPERATIVA con motivo DOCUMENTO_VENCIDO:<tipo>")
    void documentoAusentePasaUnidadAInoperativa_UNI01() {
        // Solo 3 documentos; falta HABILITACION_VEHICULAR
        List<DocumentoVehicular> docs = new ArrayList<>();
        docs.add(new DocumentoVehicular("D1", TipoDeDocumento.REVISION_TECNICA, vigenciaValida(), "REV-01"));
        docs.add(new DocumentoVehicular("D2", TipoDeDocumento.SOAT, vigenciaValida(), "SOAT-01"));
        docs.add(new DocumentoVehicular("D3", TipoDeDocumento.PERMISO_MTC, vigenciaValida(), "MTC-01"));

        Unidad unidad = crearUnidadOperativa("UNI-001", docs);
        unidad.evaluarVigenciaDocumental(hoy);

        // Invariante UNI-01: al faltar HABILITACION_VEHICULAR, queda inoperativa
        assertThat(unidad.getEstadoOperativo().situacion()).isEqualTo(SituacionOperativa.INOPERATIVA);
        assertThat(unidad.getEstadoOperativo().motivo()).isEqualTo("DOCUMENTO_VENCIDO:HABILITACION_VEHICULAR");
    }

    // =========================================================================
    // INVARIANTE UNI-02
    // "Una unidad con mantenimiento preventivo vencido no puede habilitarse."
    // =========================================================================

    @Test
    @DisplayName("UNI-02: Unidad OPERATIVA y con documentos vigentes pero con mantenimiento vencido no esta habilitada")
    void unidadConMantenimientoPreventivoVencidoNoEstaHabilitada_UNI02() {
        List<DocumentoVehicular> docs = crearCuatroDocumentosVigentes("UNI-002");
        // Proximo servicio en 20 000 km, kilometraje actual en 20 050 km -> vencido
        Unidad unidad = new Unidad(
                "UNI-002",
                new Placa("ABC-123"),
                TipoDeUnidad.FURGON,
                new Capacidad(10_000, new BigDecimal("32.00")),
                new Kilometraje(20_050),
                EstadoOperativo.operativa(),
                new ProgramaDeMantenimiento(
                        new Kilometraje(10_000),
                        new Kilometraje(20_000),
                        IntervaloDeMantenimiento.ACEITE_Y_FILTROS),
                docs);

        // Invariante UNI-02
        assertThat(unidad.estaHabilitada(hoy)).isFalse();
    }

    @Test
    @DisplayName("UNI-02 caso favorable: Unidad OPERATIVA, documentos vigentes y mantenimiento al dia esta habilitada")
    void unidadOperativaConDocumentosYMantenimientoAlDiaEstaHabilitada_UNI02() {
        List<DocumentoVehicular> docs = crearCuatroDocumentosVigentes("UNI-002");
        // Proximo servicio en 20 000 km, kilometraje actual en 15 000 km -> al dia
        Unidad unidad = new Unidad(
                "UNI-002",
                new Placa("ABC-123"),
                TipoDeUnidad.FURGON,
                new Capacidad(10_000, new BigDecimal("32.00")),
                new Kilometraje(15_000),
                EstadoOperativo.operativa(),
                new ProgramaDeMantenimiento(
                        new Kilometraje(10_000),
                        new Kilometraje(20_000),
                        IntervaloDeMantenimiento.ACEITE_Y_FILTROS),
                docs);

        assertThat(unidad.estaHabilitada(hoy)).isTrue();
    }

    // =========================================================================
    // INVARIANTE UNI-03
    // "El kilometraje nunca decrece."
    // =========================================================================

    @Test
    @DisplayName("UNI-03: actualizarKilometraje con un valor menor lanza KilometrajeRetrocedeException")
    void actualizarKilometrajeConMenorValorLanzaKilometrajeRetrocedeException_UNI03() {
        Unidad unidad = crearUnidadOperativa("UNI-003", crearCuatroDocumentosVigentes("UNI-003"));

        // Invariante UNI-03: actual es 10 000 km, intentar actualizar a 9 999 km
        assertThatThrownBy(() -> unidad.actualizarKilometraje(new Kilometraje(9_999)))
                .isInstanceOf(KilometrajeRetrocedeException.class)
                .hasMessageContaining("UNI-03");
    }

    @Test
    @DisplayName("UNI-03: actualizarKilometraje con el mismo valor no lanza excepcion")
    void actualizarKilometrajeConMismoValorNoLanza_UNI03() {
        Unidad unidad = crearUnidadOperativa("UNI-003", crearCuatroDocumentosVigentes("UNI-003"));

        unidad.actualizarKilometraje(new Kilometraje(10_000));

        assertThat(unidad.getKilometraje().valor()).isEqualTo(10_000);
    }

    @Test
    @DisplayName("actualizarKilometraje con valor mayor actualiza el kilometraje exitosamente")
    void actualizarKilometrajeConMayorValorActualizaKilometraje() {
        Unidad unidad = crearUnidadOperativa("UNI-003", crearCuatroDocumentosVigentes("UNI-003"));

        unidad.actualizarKilometraje(new Kilometraje(12_500));

        assertThat(unidad.getKilometraje().valor()).isEqualTo(12_500);
    }

    // =========================================================================
    // Motivos de no elegibilidad y otros comportamientos
    // =========================================================================

    @Test
    @DisplayName("Borde: motivosDeNoElegibilidad devuelve lista vacia en caso elegible")
    void motivosDeNoElegibilidadDevuelveListaVaciaEnCasoElegible() {
        List<DocumentoVehicular> docs = crearCuatroDocumentosVigentes("UNI-004");
        Unidad unidad = crearUnidadOperativa("UNI-004", docs);

        List<String> motivos = unidad.motivosDeNoElegibilidad(
                hoy, 5_000, new BigDecimal("15.00"), TipoDeCarga.PALETIZADA);

        assertThat(motivos).isEmpty();
    }

    @Test
    @DisplayName("Borde: motivosDeNoElegibilidad acumula dos motivos a la vez (documento vencido y mantenimiento vencido)")
    void motivosDeNoElegibilidadAcumulaDosMotivosALaVez() {
        List<DocumentoVehicular> docs = new ArrayList<>();
        docs.add(new DocumentoVehicular("D1", TipoDeDocumento.REVISION_TECNICA, vigenciaValida(), "REV-01"));
        docs.add(new DocumentoVehicular("D2", TipoDeDocumento.SOAT, vigenciaVencida(), "SOAT-01")); // vencido
        docs.add(new DocumentoVehicular("D3", TipoDeDocumento.PERMISO_MTC, vigenciaValida(), "MTC-01"));
        docs.add(new DocumentoVehicular("D4", TipoDeDocumento.HABILITACION_VEHICULAR, vigenciaValida(), "HAB-01"));

        // Kilometraje 21 000 excede proximo de 20 000
        Unidad unidad = new Unidad(
                "UNI-004",
                new Placa("ABC-123"),
                TipoDeUnidad.FURGON,
                new Capacidad(10_000, new BigDecimal("32.00")),
                new Kilometraje(21_000),
                EstadoOperativo.inoperativa("DOCUMENTO_VENCIDO:SOAT"),
                new ProgramaDeMantenimiento(
                        new Kilometraje(10_000),
                        new Kilometraje(20_000),
                        IntervaloDeMantenimiento.ACEITE_Y_FILTROS),
                docs);

        List<String> motivos = unidad.motivosDeNoElegibilidad(
                hoy, 5_000, new BigDecimal("15.00"), TipoDeCarga.PALETIZADA);

        assertThat(motivos).containsExactlyInAnyOrder("DOCUMENTO_VENCIDO:SOAT", "MANTENIMIENTO_VENCIDO");
    }

    @Test
    @DisplayName("motivosDeNoElegibilidad reporta capacidad insuficiente y tipo incompatible")
    void motivosDeNoElegibilidadReportaCapacidadInsuficienteYTipoIncompatible() {
        List<DocumentoVehicular> docs = crearCuatroDocumentosVigentes("UNI-005");
        Unidad unidad = crearUnidadOperativa("UNI-005", docs); // FURGON, 10 000 kg

        // Carga MAQUINARIA_PESADA (no admitida por FURGON) y peso 15 000 kg (excede 10 000 kg)
        List<String> motivos = unidad.motivosDeNoElegibilidad(
                hoy, 15_000, new BigDecimal("10.00"), TipoDeCarga.MAQUINARIA_PESADA);

        assertThat(motivos).contains("CAPACIDAD_INSUFICIENTE", "TIPO_INCOMPATIBLE");
    }

    @Test
    @DisplayName("marcarInoperativa fuerza estado INOPERATIVA con motivo obligatorio")
    void marcarInoperativaFuerzaEstado() {
        Unidad unidad = crearUnidadOperativa("UNI-006", crearCuatroDocumentosVigentes("UNI-006"));

        unidad.marcarInoperativa("SINIESTRO_FRONTAL");

        assertThat(unidad.getEstadoOperativo().situacion()).isEqualTo(SituacionOperativa.INOPERATIVA);
        assertThat(unidad.getEstadoOperativo().motivo()).isEqualTo("SINIESTRO_FRONTAL");

        assertThatThrownBy(() -> unidad.marcarInoperativa(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> unidad.marcarInoperativa(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("registrarDocumento reemplaza documento existente del mismo tipo")
    void registrarDocumentoReemplazaDocumentoDelMismoTipo() {
        Unidad unidad = crearUnidadOperativa("UNI-007", crearCuatroDocumentosVigentes("UNI-007"));

        PeriodoDeVigencia nuevaVigencia = new PeriodoDeVigencia(hoy, hoy.plusYears(1));
        unidad.registrarDocumento(TipoDeDocumento.SOAT, nuevaVigencia, "NUEVO-SOAT-999", hoy);

        assertThat(unidad.getDocumentos()).hasSize(4);
        DocumentoVehicular soatActual = unidad.getDocumentos().stream()
                .filter(d -> d.getTipo() == TipoDeDocumento.SOAT)
                .findFirst()
                .orElseThrow();
        assertThat(soatActual.getNumero()).isEqualTo("NUEVO-SOAT-999");
        assertThat(soatActual.getVigencia()).isEqualTo(nuevaVigencia);
    }
}
