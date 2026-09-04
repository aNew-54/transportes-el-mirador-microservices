package pe.edu.unc.elmirador.ejecucion.models.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import pe.edu.unc.elmirador.ejecucion.exceptions.CheckListNoAprobadoException;
import pe.edu.unc.elmirador.ejecucion.exceptions.ConformidadesPendientesException;
import pe.edu.unc.elmirador.ejecucion.exceptions.DominioEjecucionException;
import pe.edu.unc.elmirador.ejecucion.exceptions.EjecucionEntregadaException;
import pe.edu.unc.elmirador.ejecucion.exceptions.EvidenciaRequeridaException;
import pe.edu.unc.elmirador.ejecucion.exceptions.LiquidacionPendienteException;
import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoDeEjecucion;
import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoDeParada;
import pe.edu.unc.elmirador.ejecucion.models.vo.ResultadoDeCheckList;

/**
 * Raiz de agregado EjecucionDeViaje.
 * Comparte identidad con el viaje planificado (viajeId).
 * Sostiene las invariantes EJV-01 a EJV-05 y LIQ-04.
 */
@Entity
@Table(name = "ejecuciones")
public class EjecucionDeViaje {

    /** La identidad la comparte con el viaje planificado: no hay id propio. */
    @Id
    @Column(name = "viaje_id", length = 40, nullable = false)
    private String viajeId;

    @Column(name = "unidad_ejecutora_id", length = 40, nullable = false)
    private String unidadEjecutoraId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoDeEjecucion estado;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "checklist_id")
    private CheckListDeSalida checkList;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "ejecucion_id", nullable = false)
    @OrderBy("secuencia ASC")
    private List<Parada> paradas = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "ejecucion_id", nullable = false)
    private List<Hito> hitos = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "ejecucion_id", nullable = false)
    private List<Incidencia> incidencias = new ArrayList<>();

    /** EJV-05: el transbordo apila aqui la unidad anterior y conserva el viajeId. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ejecucion_unidades_anteriores", joinColumns = @JoinColumn(name = "ejecucion_id"))
    @Column(name = "unidad_id", length = 40, nullable = false)
    private List<String> unidadesAnteriores = new ArrayList<>();

    @Column(name = "fecha_inicio")
    private OffsetDateTime fechaInicio;

    @Column(name = "fecha_entrega")
    private OffsetDateTime fechaEntrega;

    /** Exigido por JPA. No usar: no valida ninguna invariante. */
    protected EjecucionDeViaje() {
    }

    public static EjecucionDeViaje crear(String viajeId, String unidadEjecutoraId, List<Parada> paradas) {
        return new EjecucionDeViaje(viajeId, unidadEjecutoraId, paradas);
    }

    public EjecucionDeViaje(String viajeId, String unidadEjecutoraId, List<Parada> paradas) {
        if (viajeId == null || viajeId.isBlank()) {
            throw new IllegalArgumentException("El viajeId es obligatorio");
        }
        if (unidadEjecutoraId == null || unidadEjecutoraId.isBlank()) {
            throw new IllegalArgumentException("La unidadEjecutoraId es obligatoria");
        }
        if (paradas == null || paradas.isEmpty()) {
            throw new IllegalArgumentException("Las paradas son obligatorias");
        }
        this.viajeId = viajeId.trim();
        this.unidadEjecutoraId = unidadEjecutoraId.trim();
        this.estado = EstadoDeEjecucion.PENDIENTE;
        this.checkList = null;
        this.paradas.addAll(paradas);
        
        
        
        this.fechaInicio = null;
        this.fechaEntrega = null;
    }

    public void registrarCheckList(ResultadoDeCheckList resultado) {
        if (resultado == null) {
            throw new IllegalArgumentException("El resultado del check-list es obligatorio");
        }
        if (this.estado != EstadoDeEjecucion.PENDIENTE) {
            throw new DominioEjecucionException("Solo se puede registrar el check-list en estado PENDIENTE");
        }
        this.checkList = new CheckListDeSalida("CHK-" + this.viajeId, resultado);
    }

    public void registrarCheckList(CheckListDeSalida checkList) {
        if (checkList == null) {
            throw new IllegalArgumentException("El check-list es obligatorio");
        }
        if (this.estado != EstadoDeEjecucion.PENDIENTE) {
            throw new DominioEjecucionException("Solo se puede registrar el check-list en estado PENDIENTE");
        }
        this.checkList = checkList;
    }

    public void iniciar(OffsetDateTime fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de inicio es obligatoria");
        }
        this.estado.validarTransicionHacia(EstadoDeEjecucion.EN_RUTA);
        if (this.checkList == null || !this.checkList.estaAprobado()) {
            throw new CheckListNoAprobadoException(
                "No se puede iniciar la ejecucion sin check-list de salida aprobado (EJV-01)"
            );
        }
        this.estado = EstadoDeEjecucion.EN_RUTA;
        this.fechaInicio = fecha;
    }

    public void reportarHito(Hito hito) {
        if (hito == null) {
            throw new IllegalArgumentException("El hito es obligatorio");
        }
        if (this.estado == EstadoDeEjecucion.ENTREGADA || this.estado == EstadoDeEjecucion.CERRADA) {
            throw new EjecucionEntregadaException(
                "No se pueden reportar hitos sobre una ejecucion entregada o cerrada (EJV-04)"
            );
        }
        this.hitos.add(hito);
    }

    public void registrarIncidencia(Incidencia incidencia) {
        if (incidencia == null) {
            throw new IllegalArgumentException("La incidencia es obligatoria");
        }
        if (incidencia.getTipo().exigeEvidencia() && incidencia.getEvidencia() == null) {
            throw new EvidenciaRequeridaException(
                "La incidencia de tipo " + incidencia.getTipo() + " exige evidencia obligatoria"
            );
        }
        this.incidencias.add(incidencia);
    }

    public void registrarConformidad(int secuenciaDeParada, ConformidadDeEntrega conformidad) {
        if (conformidad == null) {
            throw new IllegalArgumentException("La conformidad es obligatoria");
        }
        if (this.estado == EstadoDeEjecucion.ENTREGADA || this.estado == EstadoDeEjecucion.CERRADA) {
            throw new EjecucionEntregadaException(
                "No se pueden registrar conformidades sobre una ejecucion entregada o cerrada (EJV-04)"
            );
        }
        Parada parada = buscarParada(secuenciaDeParada);
        if (parada.getConformidad() != null || parada.getEstado() == EstadoDeParada.ATENDIDA) {
            throw new DominioEjecucionException(
                "La parada ya cuenta con una conformidad registrada (EJV-02)"
            );
        }
        if (!parada.getOrdenDeServicioId().equals(conformidad.getOrdenDeServicioId())) {
            throw new DominioEjecucionException(
                "La orden de servicio de la conformidad (" + conformidad.getOrdenDeServicioId()
                    + ") no coincide con la de la parada (" + parada.getOrdenDeServicioId() + ") (EJV-02)"
            );
        }
        parada.registrarConformidad(conformidad);
    }

    public void reabrirParada(int secuenciaDeParada) {
        if (this.estado == EstadoDeEjecucion.ENTREGADA || this.estado == EstadoDeEjecucion.CERRADA) {
            throw new EjecucionEntregadaException(
                "Una ejecucion entregada o cerrada no admite la reapertura de paradas atendidas (EJV-04)"
            );
        }
        Parada parada = buscarParada(secuenciaDeParada);
        parada.reabrir();
    }

    public void marcarEntregada(OffsetDateTime fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de entrega es obligatoria");
        }
        if (this.estado != EstadoDeEjecucion.EN_RUTA) {
            this.estado.validarTransicionHacia(EstadoDeEjecucion.ENTREGADA);
        }
        boolean todasFirmadas = !paradas.isEmpty() && paradas.stream().allMatch(Parada::tieneConformidadFirmada);
        if (!todasFirmadas) {
            throw new ConformidadesPendientesException(
                "No se puede marcar como entregada: existen paradas sin conformidad firmada (EJV-03)"
            );
        }
        this.estado = EstadoDeEjecucion.ENTREGADA;
        this.fechaEntrega = fecha;
    }

    public void transbordar(String nuevaUnidadId) {
        if (nuevaUnidadId == null || nuevaUnidadId.isBlank()) {
            throw new IllegalArgumentException("La nueva unidad es obligatoria");
        }
        String nuevaUnidadNormalizada = nuevaUnidadId.trim();
        if (nuevaUnidadNormalizada.equals(this.unidadEjecutoraId)) {
            throw new IllegalArgumentException("La nueva unidad debe ser distinta a la unidad actual: " + nuevaUnidadId);
        }
        if (this.estado == EstadoDeEjecucion.ENTREGADA || this.estado == EstadoDeEjecucion.CERRADA) {
            throw new EjecucionEntregadaException(
                "No se puede realizar un transbordo sobre una ejecucion entregada o cerrada (EJV-05)"
            );
        }
        this.unidadesAnteriores.add(this.unidadEjecutoraId);
        this.unidadEjecutoraId = nuevaUnidadNormalizada;
    }

    public void cerrar(boolean hayLiquidacionesPendientes) {
        if (this.estado != EstadoDeEjecucion.ENTREGADA) {
            this.estado.validarTransicionHacia(EstadoDeEjecucion.CERRADA);
        }
        if (hayLiquidacionesPendientes) {
            throw new LiquidacionPendienteException(
                "No se puede cerrar la ejecucion mientras existan liquidaciones pendientes (LIQ-04)"
            );
        }
        this.estado = EstadoDeEjecucion.CERRADA;
    }

    public void suspender() {
        this.estado.validarTransicionHacia(EstadoDeEjecucion.SUSPENDIDA);
        this.estado = EstadoDeEjecucion.SUSPENDIDA;
    }

    public void reanudar() {
        this.estado.validarTransicionHacia(EstadoDeEjecucion.EN_RUTA);
        this.estado = EstadoDeEjecucion.EN_RUTA;
    }

    public List<Incidencia> incidenciasSinResolver() {
        return incidencias.stream()
                .filter(i -> i.getTipo().exigeEvidencia() && !i.isResuelta())
                .toList();
    }

    private Parada buscarParada(int secuenciaDeParada) {
        return paradas.stream()
                .filter(p -> p.getSecuencia() == secuenciaDeParada)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No existe parada con secuencia " + secuenciaDeParada));
    }

    public String getViajeId() {
        return viajeId;
    }

    public String getId() {
        return viajeId;
    }

    public String getUnidadEjecutoraId() {
        return unidadEjecutoraId;
    }

    public EstadoDeEjecucion getEstado() {
        return estado;
    }

    public CheckListDeSalida getCheckList() {
        return checkList;
    }

    public List<Parada> getParadas() {
        return List.copyOf(paradas);
    }

    public List<Hito> getHitos() {
        return List.copyOf(hitos);
    }

    public List<Incidencia> getIncidencias() {
        return List.copyOf(incidencias);
    }

    public List<String> getUnidadesAnteriores() {
        return List.copyOf(unidadesAnteriores);
    }

    public OffsetDateTime getFechaInicio() {
        return fechaInicio;
    }

    public OffsetDateTime getFechaEntrega() {
        return fechaEntrega;
    }
}
