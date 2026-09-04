package pe.edu.unc.elmirador.unidades.models.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import pe.edu.unc.elmirador.unidades.exceptions.ReactivacionInvalidaException;
import pe.edu.unc.elmirador.unidades.models.vo.Capacidad;
import pe.edu.unc.elmirador.unidades.models.vo.EstadoOperativo;
import pe.edu.unc.elmirador.unidades.models.vo.Kilometraje;
import pe.edu.unc.elmirador.unidades.models.vo.MotivoDeNoElegibilidad;
import pe.edu.unc.elmirador.unidades.models.vo.PeriodoDeVigencia;
import pe.edu.unc.elmirador.unidades.models.vo.Placa;
import pe.edu.unc.elmirador.unidades.models.vo.ProgramaDeMantenimiento;
import pe.edu.unc.elmirador.unidades.models.vo.SituacionOperativa;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeCarga;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeDocumento;
import pe.edu.unc.elmirador.unidades.models.vo.TipoDeUnidad;

/**
 * Raiz del agregado Unidad. Sostiene UNI-01, UNI-02 y UNI-03.
 *
 * <p>El agregado no lee el reloj: toda operacion que dependa de "hoy" recibe la fecha de
 * evaluacion y la exige no nula. Un dominio que llama a {@code LocalDate.now()} produce
 * pruebas no deterministas y hace imposible reprocesar un hecho pasado.
 */
public class Unidad {

    private final String id;
    private final Placa placa;
    private final TipoDeUnidad tipo;
    private final Capacidad capacidad;
    private Kilometraje kilometraje;
    private EstadoOperativo estadoOperativo;
    private ProgramaDeMantenimiento programaDeMantenimiento;
    private final List<DocumentoVehicular> documentos = new ArrayList<>();

    public Unidad(
            String id,
            Placa placa,
            TipoDeUnidad tipo,
            Capacidad capacidad,
            Kilometraje kilometraje,
            EstadoOperativo estadoOperativo,
            ProgramaDeMantenimiento programaDeMantenimiento,
            List<DocumentoVehicular> documentos) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id no puede estar vacio");
        }
        if (placa == null) {
            throw new IllegalArgumentException("La placa no puede ser nula");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de unidad no puede ser nulo");
        }
        if (capacidad == null) {
            throw new IllegalArgumentException("La capacidad no puede ser nula");
        }
        if (kilometraje == null) {
            throw new IllegalArgumentException("El kilometraje no puede ser nulo");
        }
        if (programaDeMantenimiento == null) {
            throw new IllegalArgumentException(
                    "El programa de mantenimiento es obligatorio: sin el, UNI-02 no se puede evaluar");
        }
        this.id = id;
        this.placa = placa;
        this.tipo = tipo;
        this.capacidad = capacidad;
        this.kilometraje = kilometraje;
        this.estadoOperativo = estadoOperativo != null ? estadoOperativo : EstadoOperativo.operativa();
        this.programaDeMantenimiento = programaDeMantenimiento;
        if (documentos != null) {
            this.documentos.addAll(documentos);
        }
    }

    /** UNI-03: el kilometraje nunca decrece. */
    public void actualizarKilometraje(Kilometraje nuevo) {
        this.kilometraje = this.kilometraje.avanzarA(nuevo);
    }

    /**
     * Registra o renueva un documento y reevalua la vigencia documental.
     *
     * <p>La fecha de evaluacion es la fecha del negocio, NO el inicio de vigencia del documento
     * que se registra: registrar un documento ya vencido debe dejar la unidad inoperativa.
     */
    public void registrarDocumento(
            TipoDeDocumento tipoDoc, PeriodoDeVigencia vigencia, String numero, LocalDate fechaEvaluacion) {
        if (tipoDoc == null) {
            throw new IllegalArgumentException("El tipo de documento no puede ser nulo");
        }
        if (vigencia == null) {
            throw new IllegalArgumentException("El periodo de vigencia no puede ser nulo");
        }
        registrarDocumento(
                new DocumentoVehicular(this.id + "-" + tipoDoc.name(), tipoDoc, vigencia, numero), fechaEvaluacion);
    }

    public void registrarDocumento(DocumentoVehicular documento, LocalDate fechaEvaluacion) {
        if (documento == null) {
            throw new IllegalArgumentException("El documento vehicular no puede ser nulo");
        }
        exigirFecha(fechaEvaluacion);
        documentos.removeIf(d -> d.getTipo() == documento.getTipo());
        documentos.add(documento);
        evaluarVigenciaDocumental(fechaEvaluacion);
    }

    /**
     * UNI-01: con cualquiera de los cuatro documentos ausente o vencido, la unidad pasa a
     * INOPERATIVA. Es un cambio de estado, no una alerta.
     *
     * <p>No rehabilita: volver a servicio es un acto deliberado, {@link #reactivar(LocalDate)}.
     * Tampoco pisa un motivo no documental ya vigente, para no perder por que estaba parada.
     */
    public void evaluarVigenciaDocumental(LocalDate fecha) {
        exigirFecha(fecha);
        TipoDeDocumento vencido = primerDocumentoNoVigente(fecha);
        if (vencido == null || inoperativaPorMotivoNoDocumental()) {
            return;
        }
        this.estadoOperativo =
                EstadoOperativo.inoperativa(MotivoDeNoElegibilidad.DOCUMENTO_VENCIDO.codigo(vencido.name()));
    }

    /** UNI-01 y UNI-02 combinadas: estado asignable, documentos vigentes y mantenimiento al dia. */
    public boolean estaHabilitada(LocalDate fecha) {
        exigirFecha(fecha);
        return estadoOperativo.esAsignable()
                && primerDocumentoNoVigente(fecha) == null
                && !programaDeMantenimiento.estaVencido(kilometraje);
    }

    public void marcarInoperativa(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo para marcar inoperativa es obligatorio");
        }
        this.estadoOperativo = EstadoOperativo.inoperativa(motivo);
    }

    public void marcarEnTaller(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo para marcar en taller es obligatorio");
        }
        this.estadoOperativo = EstadoOperativo.enTaller(motivo);
    }

    /**
     * Devuelve la unidad a servicio. Falla si todavia hay un documento vencido (UNI-01) o el
     * mantenimiento preventivo esta vencido (UNI-02): reactivar no puede saltarse una invariante.
     */
    public void reactivar(LocalDate fecha) {
        exigirFecha(fecha);
        TipoDeDocumento vencido = primerDocumentoNoVigente(fecha);
        if (vencido != null) {
            throw new ReactivacionInvalidaException(
                    "No se puede reactivar la unidad " + id + " con documento no vigente (UNI-01): " + vencido);
        }
        if (programaDeMantenimiento.estaVencido(kilometraje)) {
            throw new ReactivacionInvalidaException(
                    "No se puede reactivar la unidad " + id + " con mantenimiento preventivo vencido (UNI-02)");
        }
        this.estadoOperativo = EstadoOperativo.operativa();
    }

    /** Cierra el ciclo de mantenimiento: reprograma el proximo servicio desde el km atendido. */
    public void registrarMantenimientoRealizado(Kilometraje kmAtencion) {
        if (kmAtencion == null) {
            throw new IllegalArgumentException("El kilometraje de atencion no puede ser nulo");
        }
        this.programaDeMantenimiento =
                ProgramaDeMantenimiento.of(kmAtencion, programaDeMantenimiento.intervalo());
    }

    /**
     * Motivos del contrato 2, en orden estable. Lista vacia significa elegible: el controlador
     * de S4 no vuelve a decidir nada, solo serializa.
     */
    public List<String> motivosDeNoElegibilidad(
            LocalDate fecha, int pesoKg, BigDecimal volumenM3, TipoDeCarga carga) {
        exigirFecha(fecha);
        List<String> motivos = new ArrayList<>();

        if (estadoOperativo.situacion() == SituacionOperativa.EN_TALLER) {
            motivos.add(MotivoDeNoElegibilidad.EN_TALLER.codigo());
        } else if (estadoOperativo.situacion() == SituacionOperativa.INOPERATIVA
                && inoperativaPorMotivoNoDocumental()) {
            motivos.add(MotivoDeNoElegibilidad.INOPERATIVA.codigo());
        }

        for (TipoDeDocumento tipoDoc : TipoDeDocumento.values()) {
            DocumentoVehicular doc = buscarDocumento(tipoDoc);
            if (doc == null || !doc.estaVigente(fecha)) {
                motivos.add(MotivoDeNoElegibilidad.DOCUMENTO_VENCIDO.codigo(tipoDoc.name()));
            }
        }

        if (programaDeMantenimiento.estaVencido(kilometraje)) {
            motivos.add(MotivoDeNoElegibilidad.MANTENIMIENTO_VENCIDO.codigo());
        }
        if (!capacidad.admite(pesoKg, volumenM3)) {
            motivos.add(MotivoDeNoElegibilidad.CAPACIDAD_INSUFICIENTE.codigo());
        }
        if (carga != null && !tipo.admite(carga)) {
            motivos.add(MotivoDeNoElegibilidad.TIPO_INCOMPATIBLE.codigo());
        }
        return List.copyOf(motivos);
    }

    private static void exigirFecha(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException(
                    "La fecha de evaluacion es obligatoria: el dominio no lee el reloj del sistema");
        }
    }

    private boolean inoperativaPorMotivoNoDocumental() {
        return estadoOperativo.situacion() == SituacionOperativa.INOPERATIVA
                && !estadoOperativo.motivo().startsWith(MotivoDeNoElegibilidad.DOCUMENTO_VENCIDO.name());
    }

    private TipoDeDocumento primerDocumentoNoVigente(LocalDate fecha) {
        for (TipoDeDocumento tipoDoc : TipoDeDocumento.values()) {
            DocumentoVehicular doc = buscarDocumento(tipoDoc);
            if (doc == null || !doc.estaVigente(fecha)) {
                return tipoDoc;
            }
        }
        return null;
    }

    private DocumentoVehicular buscarDocumento(TipoDeDocumento tipoDoc) {
        for (DocumentoVehicular doc : documentos) {
            if (doc.getTipo() == tipoDoc) {
                return doc;
            }
        }
        return null;
    }

    public String getId() {
        return id;
    }

    public Placa getPlaca() {
        return placa;
    }

    public TipoDeUnidad getTipo() {
        return tipo;
    }

    public Capacidad getCapacidad() {
        return capacidad;
    }

    public Kilometraje getKilometraje() {
        return kilometraje;
    }

    public EstadoOperativo getEstadoOperativo() {
        return estadoOperativo;
    }

    public ProgramaDeMantenimiento getProgramaDeMantenimiento() {
        return programaDeMantenimiento;
    }

    public List<DocumentoVehicular> getDocumentos() {
        return List.copyOf(documentos);
    }
}
