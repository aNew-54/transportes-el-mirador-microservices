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
import java.util.Set;
import pe.edu.unc.elmirador.ejecucion.exceptions.CheckListNoAprobadoException;
import pe.edu.unc.elmirador.ejecucion.exceptions.ConformidadesPendientesException;
import pe.edu.unc.elmirador.ejecucion.exceptions.DominioEjecucionException;
import pe.edu.unc.elmirador.ejecucion.exceptions.EjecucionEntregadaException;
import pe.edu.unc.elmirador.ejecucion.exceptions.EvidenciaRequeridaException;
import pe.edu.unc.elmirador.ejecucion.exceptions.LiquidacionPendienteException;
import pe.edu.unc.elmirador.ejecucion.models.vo.EsperaFacturable;
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

    /**
     * Los conductores que el contrato 4 asigno al viaje. Sin ellos el contrato 6 no tiene a quien
     * reportarle horas: hasta este slice llegaban en la hoja de ruta y se tiraban al suelo.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ejecucion_conductores", joinColumns = @JoinColumn(name = "ejecucion_id"))
    @Column(name = "conductor_id", length = 40, nullable = false)
    private List<String> conductorIds = new ArrayList<>();

    /** Odometro al cerrar. Nulo hasta entonces: es lo que viaja en el contrato 5. */
    @Column(name = "kilometraje_final")
    private Integer kilometrajeFinal;

    @Column(name = "fecha_inicio")
    private OffsetDateTime fechaInicio;

    @Column(name = "fecha_entrega")
    private OffsetDateTime fechaEntrega;

    /** Exigido por JPA. No usar: no valida ninguna invariante. */
    protected EjecucionDeViaje() {
    }

    public static EjecucionDeViaje crear(String viajeId, String unidadEjecutoraId,
                                        List<String> conductorIds, List<Parada> paradas) {
        return new EjecucionDeViaje(viajeId, unidadEjecutoraId, conductorIds, paradas);
    }

    public EjecucionDeViaje(String viajeId, String unidadEjecutoraId,
                           List<String> conductorIds, List<Parada> paradas) {
        if (viajeId == null || viajeId.isBlank()) {
            throw new IllegalArgumentException("El viajeId es obligatorio");
        }
        if (unidadEjecutoraId == null || unidadEjecutoraId.isBlank()) {
            throw new IllegalArgumentException("La unidadEjecutoraId es obligatoria");
        }
        if (conductorIds == null || conductorIds.isEmpty()) {
            throw new IllegalArgumentException("Los conductores asignados son obligatorios");
        }
        if (paradas == null || paradas.isEmpty()) {
            throw new IllegalArgumentException("Las paradas son obligatorias");
        }
        this.viajeId = viajeId.trim();
        this.unidadEjecutoraId = unidadEjecutoraId.trim();
        this.estado = EstadoDeEjecucion.PENDIENTE;
        this.checkList = null;
        this.paradas.addAll(paradas);
        this.conductorIds.addAll(conductorIds);
        this.kilometrajeFinal = null;
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

    /**
     * Cierra la ejecucion y fija el odometro final.
     *
     * <p>{@code hayLiquidacionesPendientes} lo calcula el servicio de aplicacion contando las
     * liquidaciones de este mismo contexto. Hasta este slice llegaba en el cuerpo de la peticion,
     * y entonces bastaba mandar {@code false} para que LIQ-04 no pudiera fallar nunca.
     */
    public void cerrar(int kilometrajeFinal, boolean hayLiquidacionesPendientes,
                       Set<String> conductoresConHoras, Set<String> ordenesConConceptos) {
        if (kilometrajeFinal <= 0) {
            throw new IllegalArgumentException("El kilometraje final debe ser positivo: " + kilometrajeFinal);
        }
        validarCoberturaDeHoras(conductoresConHoras);
        validarConceptosImputables(ordenesConConceptos);
        if (this.estado != EstadoDeEjecucion.ENTREGADA) {
            this.estado.validarTransicionHacia(EstadoDeEjecucion.CERRADA);
        }
        if (hayLiquidacionesPendientes) {
            throw new LiquidacionPendienteException(
                "No se puede cerrar la ejecucion mientras existan liquidaciones pendientes (LIQ-04)"
            );
        }
        this.kilometrajeFinal = kilometrajeFinal;
        this.estado = EstadoDeEjecucion.CERRADA;
    }

    /**
     * El conjunto de conductores con horas reportadas debe ser exactamente el asignado.
     *
     * <p>Uno de mas es un conductor que no iba en el viaje. Uno de menos son horas que no llegan a
     * CON-02, y CON-02 es lo unico que impide que un conductor acumule mas de lo normado. Un
     * conductor que no condujo reporta cero horas; no se omite. Estricto es falsable.
     */
    private void validarCoberturaDeHoras(Set<String> conductoresConHoras) {
        if (conductoresConHoras == null) {
            throw new IllegalArgumentException("Las horas por conductor son obligatorias al cerrar");
        }
        Set<String> asignados = Set.copyOf(this.conductorIds);
        if (!asignados.equals(conductoresConHoras)) {
            throw new DominioEjecucionException(
                "Las horas reportadas no cubren exactamente a los conductores asignados: se esperaba "
                    + asignados + " y llego " + conductoresConHoras);
        }
    }

    /** Un concepto facturable se imputa a una orden de este viaje, o no se imputa. */
    private void validarConceptosImputables(Set<String> ordenesConConceptos) {
        if (ordenesConConceptos == null) {
            throw new IllegalArgumentException("Los conceptos facturables son obligatorios al cerrar");
        }
        Set<String> ordenesDelViaje = paradas.stream()
                .map(Parada::getOrdenDeServicioId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        for (String orden : ordenesConConceptos) {
            if (!ordenesDelViaje.contains(orden)) {
                throw new DominioEjecucionException(
                    "El concepto facturable se imputa a la orden " + orden
                        + ", que no tiene parada en este viaje");
            }
        }
    }

    /** Las paradas con espera registrada: una llamada al contrato 7 por cada una. */
    public List<Parada> paradasConEspera() {
        return paradas.stream().filter(p -> p.getEsperaFacturable() != null).toList();
    }

    /** EJV-04: una ejecucion entregada o cerrada ya no admite registrar esperas. */
    public void registrarEspera(int secuenciaDeParada, EsperaFacturable espera) {
        if (espera == null) {
            throw new IllegalArgumentException("La espera facturable es obligatoria");
        }
        if (this.estado == EstadoDeEjecucion.ENTREGADA || this.estado == EstadoDeEjecucion.CERRADA) {
            throw new EjecucionEntregadaException(
                "No se pueden registrar esperas sobre una ejecucion entregada o cerrada (EJV-04)"
            );
        }
        buscarParada(secuenciaDeParada).registrarEsperaFacturable(espera);
    }

    /**
     * Las incidencias que Unidades debe conocer por el contrato 5. Vacia no es un error: la mayoria
     * de los viajes terminan sin averia.
     */
    public List<Incidencia> fallasDeUnidad() {
        return incidencias.stream().filter(i -> i.getTipo().esFallaDeUnidad()).toList();
    }

    /** Las incidencias que Conductores debe conocer por el contrato 6. */
    public List<Incidencia> incidenciasImputablesAlConductor() {
        return incidencias.stream().filter(i -> i.getTipo().esImputableAlConductor()).toList();
    }

    /** Las paradas con conformidad registrada: una llamada al contrato 8 por cada una. */
    public List<Parada> paradasAtendidas() {
        return paradas.stream().filter(p -> p.getConformidad() != null).toList();
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

    public List<String> getConductorIds() {
        return List.copyOf(conductorIds);
    }

    public Integer getKilometrajeFinal() {
        return kilometrajeFinal;
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
