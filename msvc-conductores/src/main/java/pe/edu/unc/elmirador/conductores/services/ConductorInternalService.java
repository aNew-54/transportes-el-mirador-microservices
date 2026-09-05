package pe.edu.unc.elmirador.conductores.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unc.elmirador.conductores.dto.internal.request.ReportarHorasRequest;
import pe.edu.unc.elmirador.conductores.dto.internal.request.ReportarIncidenciaRequest;
import pe.edu.unc.elmirador.conductores.dto.internal.response.ElegibilidadResponse;
import pe.edu.unc.elmirador.conductores.dto.internal.response.HorasRegistradasResponse;
import pe.edu.unc.elmirador.conductores.dto.internal.response.IncidenciaRegistradaResponse;
import pe.edu.unc.elmirador.conductores.dto.response.ResultadoIdempotente;
import pe.edu.unc.elmirador.conductores.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.conductores.models.entity.Conductor;
import pe.edu.unc.elmirador.conductores.models.entity.Incidencia;
import pe.edu.unc.elmirador.conductores.models.entity.PeticionIdempotente;
import pe.edu.unc.elmirador.conductores.models.vo.CategoriaDeLicencia;
import pe.edu.unc.elmirador.conductores.models.vo.TipoDeUnidad;
import pe.edu.unc.elmirador.conductores.repositories.ConductorRepository;
import pe.edu.unc.elmirador.conductores.repositories.PeticionIdempotenteRepository;

/**
 * Lado proveedor de los contratos 3 y 6.
 *
 * <p>Va aparte de {@link ConductorService} a proposito: son dos audiencias, dos contratos y dos ritmos
 * de cambio. Un cambio pedido por Programacion no debe poder mover la API publica.
 */
@Service
public class ConductorInternalService {

    private static final BigDecimal MINUTOS_POR_HORA = new BigDecimal("60");

    private final ConductorRepository repositorio;
    private final PeticionIdempotenteRepository idempotencia;
    private final Clock reloj;

    public ConductorInternalService(
            ConductorRepository repositorio,
            PeticionIdempotenteRepository idempotencia,
            Clock reloj
    ) {
        this.repositorio = repositorio;
        this.idempotencia = idempotencia;
        this.reloj = reloj;
    }

    /**
     * Contrato 3. Las tres invariantes del contexto se evaluan en el agregado; aqui solo se le pasa la
     * ventana convertida a horas y se da forma a lo que devuelve.
     *
     * <p>{@code clienteId} es opcional: sin el, CON-03 no se evalua, porque la induccion sólo la exigen
     * algunos destinos.
     */
    @Transactional(readOnly = true)
    public ElegibilidadResponse elegibilidad(
            String conductorId,
            OffsetDateTime desde,
            OffsetDateTime hasta,
            TipoDeUnidad tipoUnidad,
            String clienteId
    ) {
        Conductor conductor = buscar(conductorId);
        List<String> motivos = conductor.motivosDeNoElegibilidad(
                desde.toLocalDate(), tipoUnidad, horasDe(desde, hasta), clienteId);

        return new ElegibilidadResponse(
                conductor.getId(),
                motivos.isEmpty(),
                motivos,
                codigoDeCategoria(conductor.getCategoriaDeLicencia()),
                conductor.getHorasAcumuladas().disponibles()
        );
    }

    /**
     * Contrato 6 · horas de conduccion. Idempotente por {@code Idempotency-Key} (regla 6).
     *
     * <p>El reintento se resuelve ANTES de tocar el agregado, y el registro de la clave va en la misma
     * transaccion que el efecto: separarlos dejaria una ventana en la que las horas ya se acumularon y
     * la clave no consta, y el siguiente reintento las sumaria dos veces.
     *
     * <p>CON-02 la sostiene {@code acumularHoras}: si el total superara el maximo normado lanza
     * {@code HorasExcedidasException}, que el manejador traduce a {@code 409} tal como pide el contrato.
     */
    @Transactional
    public ResultadoIdempotente<HorasRegistradasResponse> reportarHoras(
            String conductorId, String clave, ReportarHorasRequest peticion) {

        Optional<PeticionIdempotente> yaVista = idempotencia.findById(clave);
        if (yaVista.isPresent()) {
            return new ResultadoIdempotente<>(
                    horasDe(buscar(yaVista.get().getRecursoId()), peticion.viajeId()), true);
        }

        Conductor conductor = buscar(conductorId);
        conductor.acumularHoras(peticion.horas(), peticion.desde().toLocalDate());
        repositorio.save(conductor);
        idempotencia.save(new PeticionIdempotente(clave, conductor.getId(), OffsetDateTime.now(reloj)));

        return new ResultadoIdempotente<>(horasDe(conductor, peticion.viajeId()), false);
    }

    /** Contrato 6 · incidencia. Idempotente por {@code Idempotency-Key}, igual que las horas. */
    @Transactional
    public ResultadoIdempotente<IncidenciaRegistradaResponse> reportarIncidencia(
            String conductorId, String clave, ReportarIncidenciaRequest peticion) {

        Optional<PeticionIdempotente> yaVista = idempotencia.findById(clave);
        if (yaVista.isPresent()) {
            return new ResultadoIdempotente<>(new IncidenciaRegistradaResponse(
                    yaVista.get().getRecursoId(), conductorId, peticion.viajeId()), true);
        }

        Conductor conductor = buscar(conductorId);
        Incidencia incidencia = new Incidencia(
                UUID.randomUUID().toString(),
                peticion.viajeId(),
                peticion.tipo(),
                peticion.descripcion(),
                peticion.atribuible(),
                OffsetDateTime.now(reloj));

        conductor.registrarIncidencia(incidencia);
        repositorio.save(conductor);
        idempotencia.save(new PeticionIdempotente(clave, incidencia.getId(), OffsetDateTime.now(reloj)));

        return new ResultadoIdempotente<>(new IncidenciaRegistradaResponse(
                incidencia.getId(), conductor.getId(), peticion.viajeId()), false);
    }

    /**
     * Horas que ocupara la ventana pedida. Es una conversion de unidades, no una regla: cuantas horas
     * consume de verdad un tramo lo mide Ejecucion y lo reporta por el contrato 6.
     */
    private static BigDecimal horasDe(OffsetDateTime desde, OffsetDateTime hasta) {
        long minutos = Duration.between(desde, hasta).toMinutes();
        if (minutos < 0) {
            throw new IllegalArgumentException("La ventana termina antes de empezar: " + desde + " a " + hasta);
        }
        return BigDecimal.valueOf(minutos).divide(MINUTOS_POR_HORA, 2, RoundingMode.HALF_UP);
    }

    /** El contrato escribe la categoria con guion; el enumerado la lleva con guion bajo (regla 13). */
    private static String codigoDeCategoria(CategoriaDeLicencia categoria) {
        return categoria.name().replace('_', '-');
    }

    private HorasRegistradasResponse horasDe(Conductor conductor, String viajeId) {
        var acumuladas = conductor.getHorasAcumuladas();
        return new HorasRegistradasResponse(
                conductor.getId(),
                viajeId,
                acumuladas.horas(),
                acumuladas.disponibles(),
                acumuladas.ventanaDeComputo().desde());
    }

    private Conductor buscar(String id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("conductor", id));
    }
}
