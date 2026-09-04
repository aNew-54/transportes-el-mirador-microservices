package pe.edu.unc.elmirador.conductores.models.entity;

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

public class Conductor {

    private final String id;
    private final String nombreCompleto;
    private NumeroDeLicencia numeroDeLicencia;
    private CategoriaDeLicencia categoriaDeLicencia;
    private PeriodoDeVigencia vigenciaLicencia;
    private HorasDeConduccion horasAcumuladas;
    private EstadoDeHabilitacion estado;
    private final List<Induccion> inducciones;

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
        this.inducciones = new ArrayList<>(inducciones);
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

        if (!this.horasAcumuladas.ventanaDeComputo().estaVigenteEn(fecha)) {
            PeriodoDeVigencia nuevaVentana = new PeriodoDeVigencia(fecha, fecha.plusDays(1));
            HorasDeConduccion base = new HorasDeConduccion(
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    nuevaVentana
            );
            this.horasAcumuladas = base.acumular(horas);
        } else {
            this.horasAcumuladas = this.horasAcumuladas.acumular(horas);
        }
    }

    public void registrarDescanso(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha no puede ser nula");
        }
        PeriodoDeVigencia nuevaVentana = new PeriodoDeVigencia(fecha, fecha.plusDays(1));
        this.horasAcumuladas = new HorasDeConduccion(
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                nuevaVentana
        );
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

    public void registrarInduccion(Induccion induccion, LocalDate fecha) {
        if (induccion == null) {
            throw new IllegalArgumentException("La induccion no puede ser nula");
        }
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha no puede ser nula");
        }
        this.inducciones.removeIf(i -> i.getClienteId().equalsIgnoreCase(induccion.getClienteId()));
        this.inducciones.add(induccion);
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

    public Optional<Induccion> buscarInduccion(String clienteId) {
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
