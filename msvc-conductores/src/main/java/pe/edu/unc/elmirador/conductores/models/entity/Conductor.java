package pe.edu.unc.elmirador.conductores.models.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import pe.edu.unc.elmirador.conductores.exceptions.RehabilitacionInvalidaException;
import pe.edu.unc.elmirador.conductores.models.vo.CategoriaDeLicencia;
import pe.edu.unc.elmirador.conductores.models.vo.EstadoDeHabilitacion;
import pe.edu.unc.elmirador.conductores.models.vo.HorasDeConduccion;
import pe.edu.unc.elmirador.conductores.models.vo.MotivoDeNoElegibilidad;
import pe.edu.unc.elmirador.conductores.models.vo.NumeroDeLicencia;
import pe.edu.unc.elmirador.conductores.models.vo.PeriodoDeVigencia;
import pe.edu.unc.elmirador.conductores.models.vo.TipoDeUnidad;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "conductores")
public class Conductor {

    @Id
    @Column(name = "id", length = 40, nullable = false)
    private String id;

    @Column(name = "nombre_completo", length = 200, nullable = false)
    private String nombreCompleto;

    @Embedded
    @AttributeOverride(name = "valor", column = @Column(name = "numero_licencia", length = 9, nullable = false))
    private NumeroDeLicencia numeroDeLicencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria_licencia", length = 10, nullable = false)
    private CategoriaDeLicencia categoriaDeLicencia;

    // Tres periodos de vigencia distintos conviven en este agregado. Sin renombrar sus columnas,
    // los tres piden "desde" y "hasta" y el mapeo choca.
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "desde", column = @Column(name = "licencia_desde", nullable = false)),
        @AttributeOverride(name = "hasta", column = @Column(name = "licencia_hasta", nullable = false))
    })
    private PeriodoDeVigencia vigenciaLicencia;

    // Embebido anidado: HorasDeConduccion contiene a su vez un PeriodoDeVigencia.
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "horas", column = @Column(name = "horas_acumuladas", precision = 5, scale = 2, nullable = false)),
        @AttributeOverride(name = "ventanaDeComputo.desde", column = @Column(name = "ventana_desde", nullable = false)),
        @AttributeOverride(name = "ventanaDeComputo.hasta", column = @Column(name = "ventana_hasta", nullable = false))
    })
    private HorasDeConduccion horasAcumuladas;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "situacion", column = @Column(name = "situacion", length = 20, nullable = false)),
        @AttributeOverride(name = "motivo", column = @Column(name = "motivo_habilitacion", length = 300))
    })
    private EstadoDeHabilitacion estado;

    // La induccion es entidad hija del agregado: se guarda y se borra con el conductor, y no
    // tiene repositorio propio. orphanRemoval traduce esa pertenencia al mapeo.
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "conductor_id", nullable = false)
    private List<Induccion> inducciones = new ArrayList<>();

    // Las incidencias llegan por el contrato 6 y forman parte del legajo. No sostienen ninguna
    // invariante: se acumulan y se consultan.
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "conductor_id", nullable = false)
    private List<Incidencia> incidencias = new ArrayList<>();

    /** Exigido por JPA. No usar: no valida ninguna invariante. */
    protected Conductor() {
    }

    public Conductor(
            String id,
            String nombreCompleto,
            NumeroDeLicencia numeroDeLicencia,
            CategoriaDeLicencia categoriaDeLicencia,
            PeriodoDeVigencia vigenciaLicencia,
            HorasDeConduccion horasAcumuladas,
            EstadoDeHabilitacion estado,
            List<Induccion> inducciones
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id del conductor no puede ser nulo ni vacio");
        }
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            throw new IllegalArgumentException("El nombre completo no puede ser nulo ni vacio");
        }
        if (numeroDeLicencia == null) {
            throw new IllegalArgumentException("El numero de licencia no puede ser nulo");
        }
        if (categoriaDeLicencia == null) {
            throw new IllegalArgumentException("La categoria de licencia no puede ser nula");
        }
        if (vigenciaLicencia == null) {
            throw new IllegalArgumentException("La vigencia de licencia no puede ser nula");
        }
        if (horasAcumuladas == null) {
            throw new IllegalArgumentException("Las horas acumuladas no pueden ser nulas");
        }
        if (estado == null) {
            throw new IllegalArgumentException("El estado de habilitacion no puede ser nulo");
        }
        if (inducciones == null) {
            throw new IllegalArgumentException("La lista de inducciones no puede ser nula");
        }

        this.id = id.trim();
        this.nombreCompleto = nombreCompleto.trim();
        this.numeroDeLicencia = numeroDeLicencia;
        this.categoriaDeLicencia = categoriaDeLicencia;
        this.vigenciaLicencia = vigenciaLicencia;
        this.horasAcumuladas = horasAcumuladas;
        this.estado = estado;
        this.inducciones.addAll(inducciones);
    }

    public boolean estaHabilitadoPara(
            LocalDate fecha,
            TipoDeUnidad tipo,
            BigDecimal horasRequeridas,
            String clienteId
    ) {
        return motivosDeNoElegibilidad(fecha, tipo, horasRequeridas, clienteId).isEmpty();
    }

    public List<String> motivosDeNoElegibilidad(
            LocalDate fecha,
            TipoDeUnidad tipo,
            BigDecimal horasRequeridas,
            String clienteId
    ) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha no puede ser nula");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de unidad no puede ser nulo");
        }
        if (horasRequeridas == null) {
            throw new IllegalArgumentException("Las horas requeridas no pueden ser nulas");
        }
        if (horasRequeridas.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Las horas requeridas no pueden ser negativas");
        }

        List<String> motivos = new ArrayList<>();

        if (!estado.estaHabilitado()) {
            motivos.add(MotivoDeNoElegibilidad.NO_HABILITADO.codigo());
        }

        // CON-01: Licencia vigente y categoria suficiente
        if (!vigenciaLicencia.estaVigenteEn(fecha)) {
            motivos.add(MotivoDeNoElegibilidad.LICENCIA_VENCIDA.codigo());
        }
        if (!categoriaDeLicencia.habilitaPara(tipo)) {
            motivos.add(MotivoDeNoElegibilidad.CATEGORIA_INSUFICIENTE.codigo());
        }

        // CON-02: Horas disponibles
        if (!horasAcumuladas.tieneDisponibles(horasRequeridas)) {
            motivos.add(MotivoDeNoElegibilidad.HORAS_INSUFICIENTES.codigo());
        }

        // CON-03: Induccion de seguridad requerida por el cliente destino
        if (clienteId != null && !clienteId.isBlank()) {
            String cliente = clienteId.trim();
            Optional<Induccion> induccionOpt = buscarInduccion(cliente);
            if (induccionOpt.isEmpty() || !induccionOpt.get().estaVigenteEn(fecha)) {
                motivos.add(MotivoDeNoElegibilidad.INDUCCION_VENCIDA.codigo(cliente));
            }
        }

        return Collections.unmodifiableList(motivos);
    }

    public void acumularHoras(BigDecimal horas, LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha no puede ser nula");
        }
        if (horas == null) {
            throw new IllegalArgumentException("Las horas no pueden ser nulas");
        }
        if (horas.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Las horas a acumular no pueden ser negativas");
        }

        HorasDeConduccion base = this.horasAcumuladas.cubre(fecha)
                ? this.horasAcumuladas
                : HorasDeConduccion.ventanaDe(fecha);
        this.horasAcumuladas = base.acumular(horas);
    }

    public void registrarDescanso(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha no puede ser nula");
        }
        this.horasAcumuladas = HorasDeConduccion.ventanaDe(fecha);
    }

    public void renovarLicencia(
            NumeroDeLicencia nuevaLicencia,
            CategoriaDeLicencia nuevaCategoria,
            PeriodoDeVigencia nuevaVigencia
    ) {
        if (nuevaLicencia == null) {
            throw new IllegalArgumentException("El numero de licencia no puede ser nulo");
        }
        if (nuevaCategoria == null) {
            throw new IllegalArgumentException("La categoria de licencia no puede ser nula");
        }
        if (nuevaVigencia == null) {
            throw new IllegalArgumentException("La vigencia de licencia no puede ser nula");
        }
        this.numeroDeLicencia = nuevaLicencia;
        this.categoriaDeLicencia = nuevaCategoria;
        this.vigenciaLicencia = nuevaVigencia;
    }

    /**
     * Registra o renueva la induccion de un cliente. No recibe fecha a proposito: CON-03 no es un
     * estado almacenado sino una evaluacion por cliente, y se resuelve al consultar la elegibilidad.
     * Registrar una induccion ya vencida no habilita a nadie.
     */
    public void registrarInduccion(Induccion induccion) {
        if (induccion == null) {
            throw new IllegalArgumentException("La induccion no puede ser nula");
        }
        this.inducciones.removeIf(i -> i.getClienteId().equalsIgnoreCase(induccion.getClienteId()));
        this.inducciones.add(induccion);
    }

    /**
     * Registra una incidencia de ruta. Llega por el contrato 6, que Ejecucion empuja al cerrar el
     * viaje. No cambia la habilitacion: si una incidencia debe suspender al conductor, eso lo decide
     * quien lo suspende, y lo hace con {@link #suspender(String)}. Registrar no es sancionar.
     */
    public void registrarIncidencia(Incidencia incidencia) {
        if (incidencia == null) {
            throw new IllegalArgumentException("La incidencia no puede ser nula");
        }
        this.incidencias.add(incidencia);
    }

    public void suspender(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo de suspension no puede ser nulo ni vacio");
        }
        this.estado = EstadoDeHabilitacion.suspendido(motivo);
    }

    public void rehabilitar(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha no puede ser nula");
        }
        if (!this.vigenciaLicencia.estaVigenteEn(fecha)) {
            throw new RehabilitacionInvalidaException(
                    "No se puede rehabilitar al conductor porque su licencia no esta vigente en la fecha " + fecha
            );
        }
        this.estado = EstadoDeHabilitacion.habilitado();
    }

    private Optional<Induccion> buscarInduccion(String clienteId) {
        if (clienteId == null || clienteId.isBlank()) {
            return Optional.empty();
        }
        return inducciones.stream()
                .filter(i -> i.getClienteId().equalsIgnoreCase(clienteId.trim()))
                .findFirst();
    }

    public String getId() {
        return id;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public NumeroDeLicencia getNumeroDeLicencia() {
        return numeroDeLicencia;
    }

    public CategoriaDeLicencia getCategoriaDeLicencia() {
        return categoriaDeLicencia;
    }

    public PeriodoDeVigencia getVigenciaLicencia() {
        return vigenciaLicencia;
    }

    public HorasDeConduccion getHorasAcumuladas() {
        return horasAcumuladas;
    }

    public EstadoDeHabilitacion getEstado() {
        return estado;
    }

    public List<Incidencia> getIncidencias() {
        return Collections.unmodifiableList(incidencias);
    }

    public List<Induccion> getInducciones() {
        return List.copyOf(inducciones);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Conductor conductor = (Conductor) o;
        return Objects.equals(id, conductor.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
