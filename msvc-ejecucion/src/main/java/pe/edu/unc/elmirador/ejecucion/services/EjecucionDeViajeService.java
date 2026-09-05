package pe.edu.unc.elmirador.ejecucion.services;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unc.elmirador.ejecucion.dto.request.CerrarEjecucionRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.ConformidadRequest;
import pe.edu.unc.elmirador.ejecucion.clients.ComercialGateway;
import pe.edu.unc.elmirador.ejecucion.clients.ConductoresGateway;
import pe.edu.unc.elmirador.ejecucion.clients.FacturacionGateway;
import pe.edu.unc.elmirador.ejecucion.clients.HojaDeRutaDeViaje;
import pe.edu.unc.elmirador.ejecucion.clients.ProgramacionGateway;
import pe.edu.unc.elmirador.ejecucion.clients.UnidadesGateway;
import pe.edu.unc.elmirador.ejecucion.clients.dto.ConceptoFacturableRemoto;
import pe.edu.unc.elmirador.ejecucion.clients.dto.ConformidadPeticion;
import pe.edu.unc.elmirador.ejecucion.clients.dto.EsperaPeticion;
import pe.edu.unc.elmirador.ejecucion.clients.dto.FallaPeticion;
import pe.edu.unc.elmirador.ejecucion.clients.dto.HorasConduccionPeticion;
import pe.edu.unc.elmirador.ejecucion.clients.dto.IncidenciaPeticion;
import pe.edu.unc.elmirador.ejecucion.clients.dto.KilometrajePeticion;
import pe.edu.unc.elmirador.ejecucion.dto.request.ConceptoFacturableRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.HorasDeConductorRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.RegistrarEsperaRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.CrearEjecucionRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.RegistrarCheckListRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.RegistrarIncidenciaRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.ReportarHitoRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.TransbordoRequest;
import pe.edu.unc.elmirador.ejecucion.dto.response.EjecucionDeViajeResponse;
import pe.edu.unc.elmirador.ejecucion.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.ejecucion.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.ejecucion.mappers.EjecucionDeViajeMapper;
import pe.edu.unc.elmirador.ejecucion.models.entity.ConformidadDeEntrega;
import pe.edu.unc.elmirador.ejecucion.models.entity.EjecucionDeViaje;
import pe.edu.unc.elmirador.ejecucion.models.entity.Hito;
import pe.edu.unc.elmirador.ejecucion.models.entity.Incidencia;
import pe.edu.unc.elmirador.ejecucion.models.entity.Parada;
import pe.edu.unc.elmirador.ejecucion.models.vo.EsperaFacturable;
import pe.edu.unc.elmirador.ejecucion.models.vo.EstadoDeLiquidacion;
import pe.edu.unc.elmirador.ejecucion.models.vo.Evidencia;
import pe.edu.unc.elmirador.ejecucion.models.vo.ResultadoDeCheckList;
import pe.edu.unc.elmirador.ejecucion.repositories.EjecucionDeViajeRepository;
import pe.edu.unc.elmirador.ejecucion.repositories.LiquidacionDeViajeRepository;

@Service
public class EjecucionDeViajeService {

    private final EjecucionDeViajeRepository repository;
    private final LiquidacionDeViajeRepository liquidaciones;
    private final Clock clock;

    private final ProgramacionGateway programacionGateway;
    private final UnidadesGateway unidadesGateway;
    private final ConductoresGateway conductoresGateway;
    private final ComercialGateway comercialGateway;
    private final FacturacionGateway facturacionGateway;

    public EjecucionDeViajeService(EjecucionDeViajeRepository repository,
                                   LiquidacionDeViajeRepository liquidaciones,
                                   ProgramacionGateway programacionGateway,
                                   UnidadesGateway unidadesGateway,
                                   ConductoresGateway conductoresGateway,
                                   ComercialGateway comercialGateway,
                                   FacturacionGateway facturacionGateway,
                                   Clock clock) {
        this.repository = repository;
        this.liquidaciones = liquidaciones;
        this.programacionGateway = programacionGateway;
        this.unidadesGateway = unidadesGateway;
        this.conductoresGateway = conductoresGateway;
        this.comercialGateway = comercialGateway;
        this.facturacionGateway = facturacionGateway;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public EjecucionDeViajeResponse obtener(String viajeId) {
        EjecucionDeViaje ejecucion = repository.findById(viajeId)
                .orElseThrow(() -> new RecursoNoEncontradoException("EjecucionDeViaje", viajeId));
        return EjecucionDeViajeMapper.mapear(ejecucion);
    }

    @Transactional
    public EjecucionDeViajeResponse crear(CrearEjecucionRequest request) {
        if (repository.existsById(request.viajeId())) {
            throw new ConflictoDeRecursoException("Ya existe una ejecucion para el viaje " + request.viajeId());
        }

        // Contrato 4. La unidad y las paradas son de la hoja de ruta, no de quien abre la ejecucion.
        // Si Programacion no responde, el gateway lanza y la ejecucion no se abre: ejecutar un viaje
        // contra una hoja de ruta que nadie ha confirmado es peor que no ejecutarlo.
        HojaDeRutaDeViaje hoja = programacionGateway.obtenerHojaDeRuta(request.viajeId());

        List<Parada> paradas = hoja.paradas().stream()
                .map(p -> new Parada(p.secuencia(), p.ordenDeServicioId(), p.direccion()))
                .toList();

        // Los conductorIds llegaban en la hoja de ruta desde S5 y se descartaban aqui. Sin ellos el
        // contrato 6 no tiene a quien reportarle horas al cerrar.
        EjecucionDeViaje ejecucion = new EjecucionDeViaje(
                request.viajeId(), hoja.unidadId(), hoja.conductorIds(), paradas);
        repository.save(ejecucion);
        
        return EjecucionDeViajeMapper.mapear(ejecucion);
    }

    @Transactional
    public EjecucionDeViajeResponse registrarCheckList(String viajeId, RegistrarCheckListRequest request) {
        EjecucionDeViaje ejecucion = repository.findById(viajeId)
                .orElseThrow(() -> new RecursoNoEncontradoException("EjecucionDeViaje", viajeId));

        ResultadoDeCheckList resultado = new ResultadoDeCheckList(
                request.aprobado(), request.observaciones(), OffsetDateTime.now(clock));
        
        ejecucion.registrarCheckList(resultado);
        repository.save(ejecucion);
        
        return EjecucionDeViajeMapper.mapear(ejecucion);
    }

    @Transactional
    public EjecucionDeViajeResponse iniciar(String viajeId) {
        EjecucionDeViaje ejecucion = repository.findById(viajeId)
                .orElseThrow(() -> new RecursoNoEncontradoException("EjecucionDeViaje", viajeId));

        ejecucion.iniciar(OffsetDateTime.now(clock));
        repository.save(ejecucion);
        
        return EjecucionDeViajeMapper.mapear(ejecucion);
    }

    @Transactional
    public EjecucionDeViajeResponse reportarHito(String viajeId, ReportarHitoRequest request) {
        EjecucionDeViaje ejecucion = repository.findById(viajeId)
                .orElseThrow(() -> new RecursoNoEncontradoException("EjecucionDeViaje", viajeId));

        Hito hito = new Hito(UUID.randomUUID().toString(), request.tipo(), OffsetDateTime.now(clock), request.ubicacion());
        ejecucion.reportarHito(hito);
        repository.save(ejecucion);
        
        return EjecucionDeViajeMapper.mapear(ejecucion);
    }

    @Transactional
    public EjecucionDeViajeResponse registrarIncidencia(String viajeId, RegistrarIncidenciaRequest request) {
        EjecucionDeViaje ejecucion = repository.findById(viajeId)
                .orElseThrow(() -> new RecursoNoEncontradoException("EjecucionDeViaje", viajeId));

        Evidencia evidencia = null;
        if (request.fotografias() != null && !request.fotografias().isEmpty()) {
            evidencia = new Evidencia(request.fotografias(), request.descripcion(), OffsetDateTime.now(clock));
        }

        Incidencia incidencia = new Incidencia(UUID.randomUUID().toString(), request.tipo(), request.descripcion(), evidencia, OffsetDateTime.now(clock));
        ejecucion.registrarIncidencia(incidencia);
        repository.save(ejecucion);
        
        return EjecucionDeViajeMapper.mapear(ejecucion);
    }

    @Transactional
    public EjecucionDeViajeResponse transbordar(String viajeId, TransbordoRequest request) {
        EjecucionDeViaje ejecucion = repository.findById(viajeId)
                .orElseThrow(() -> new RecursoNoEncontradoException("EjecucionDeViaje", viajeId));

        ejecucion.transbordar(request.nuevaUnidadId());
        repository.save(ejecucion);
        
        return EjecucionDeViajeMapper.mapear(ejecucion);
    }

    @Transactional
    public EjecucionDeViajeResponse registrarConformidad(String viajeId, int secuencia, ConformidadRequest request) {
        EjecucionDeViaje ejecucion = repository.findById(viajeId)
                .orElseThrow(() -> new RecursoNoEncontradoException("EjecucionDeViaje", viajeId));

        String ordenDeServicioId = ejecucion.getParadas().stream()
                .filter(p -> p.getSecuencia() == secuencia)
                .map(Parada::getOrdenDeServicioId)
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Parada", String.valueOf(secuencia)));

        ConformidadDeEntrega conformidad = new ConformidadDeEntrega(
                UUID.randomUUID().toString(),
                ordenDeServicioId,
                request.estado(),
                request.recibidoPor(),
                OffsetDateTime.now(clock),
                request.observaciones() != null ? request.observaciones() : ""
        );

        ejecucion.registrarConformidad(secuencia, conformidad);
        
        repository.save(ejecucion);
        return EjecucionDeViajeMapper.mapear(ejecucion);
    }

    @Transactional
    public EjecucionDeViajeResponse registrarEspera(String viajeId, int secuencia, RegistrarEsperaRequest request) {
        EjecucionDeViaje ejecucion = repository.findById(viajeId)
                .orElseThrow(() -> new RecursoNoEncontradoException("EjecucionDeViaje", viajeId));

        ejecucion.registrarEspera(secuencia,
                new EsperaFacturable(request.inicio(), request.fin(), request.tiempoLibreHoras()));
        repository.save(ejecucion);

        return EjecucionDeViajeMapper.mapear(ejecucion);
    }

    /**
     * Cierra el viaje y rinde cuentas a Unidades, Conductores, Comercial y Facturacion.
     *
     * <p>El orden importa en sus dos mitades. Los empujes van <em>despues</em> de que el agregado
     * acepte cerrar: si fueran antes, un viaje que no se puede cerrar habria emitido igualmente su
     * kilometraje y sus conformidades, y los otros contextos habrian aprendido un hecho que no
     * ocurrio. Y van <em>dentro</em> de la transaccion: si un empuje falla, esta revierte y la
     * ejecucion sigue en ENTREGADA. El reintento es seguro porque las claves de idempotencia se
     * derivan del hecho reportado y no de un UUID, asi que lo ya entregado responde 200 y no se
     * duplica. Cerrar y dejar los reportes «para luego» seria peor que fallar: nadie los
     * reintentaria.
     */
    @Transactional
    public EjecucionDeViajeResponse cerrar(String viajeId, CerrarEjecucionRequest request) {
        EjecucionDeViaje ejecucion = repository.findById(viajeId)
                .orElseThrow(() -> new RecursoNoEncontradoException("EjecucionDeViaje", viajeId));

        // LIQ-04. Hasta este slice llegaba como booleano en el cuerpo, asi que bastaba mandar false
        // para que la invariante no pudiera fallar nunca. Las liquidaciones son de este contexto.
        boolean hayPendientes = !liquidaciones
                .findByViajeIdAndEstadoNot(viajeId, EstadoDeLiquidacion.APROBADA).isEmpty();

        ejecucion.cerrar(
                request.kilometrajeFinal(),
                hayPendientes,
                idsDeConductores(request.horasPorConductor()),
                ordenesImputadas(request.conceptosFacturables()));

        reportarAUnidades(ejecucion);
        reportarAConductores(ejecucion, request.horasPorConductor());
        reportarAComercial(ejecucion);
        reportarAFacturacion(ejecucion, request.conceptosFacturables());

        repository.save(ejecucion);
        return EjecucionDeViajeMapper.mapear(ejecucion);
    }

    /** Contrato 5: el odometro final y las averias que dejan la unidad fuera de servicio. */
    private void reportarAUnidades(EjecucionDeViaje ejecucion) {
        String unidadId = ejecucion.getUnidadEjecutoraId();
        unidadesGateway.reportarKilometraje(unidadId, new KilometrajePeticion(
                ejecucion.getViajeId(), ejecucion.getKilometrajeFinal(), ejecucion.getFechaEntrega()));

        for (Incidencia falla : ejecucion.fallasDeUnidad()) {
            unidadesGateway.reportarFalla(unidadId, falla.getId(), new FallaPeticion(
                    ejecucion.getViajeId(),
                    falla.getTipo().name(),
                    falla.getDescripcion(),
                    falla.getMomento(),
                    falla.dejaUnidadInoperativa()));
        }
    }

    /** Contrato 6: las horas de cada conductor y las incidencias que le son imputables. */
    private void reportarAConductores(EjecucionDeViaje ejecucion, List<HorasDeConductorRequest> horas) {
        for (HorasDeConductorRequest hora : horas) {
            conductoresGateway.reportarHoras(hora.conductorId(), new HorasConduccionPeticion(
                    ejecucion.getViajeId(), hora.horas(), hora.desde(), hora.hasta()));
        }

        for (String conductorId : ejecucion.getConductorIds()) {
            for (Incidencia incidencia : ejecucion.incidenciasImputablesAlConductor()) {
                conductoresGateway.reportarIncidencia(conductorId, incidencia.getId(), new IncidenciaPeticion(
                        ejecucion.getViajeId(),
                        incidencia.getTipo().name(),
                        incidencia.getDescripcion(),
                        !incidencia.isResuelta()));
            }
        }
    }

    /**
     * Contrato 7: las esperas facturables. El excedente lo calcula el VO y Comercial lo consume sin
     * recalcularlo. La diferencia de carga —la otra mitad del contrato— no se empuja: Ejecucion no
     * tiene, en ningun sitio del agregado, lo declarado ni lo real. Cablearla es un slice propio.
     */
    private void reportarAComercial(EjecucionDeViaje ejecucion) {
        for (Parada parada : ejecucion.paradasConEspera()) {
            EsperaFacturable espera = parada.getEsperaFacturable();
            comercialGateway.reportarEspera(parada.getOrdenDeServicioId(), new EsperaPeticion(
                    ejecucion.getViajeId(),
                    PUNTO_DE_ESPERA,
                    espera.tiempoLibreHoras(),
                    espera.tiempoRealHoras().doubleValue(),
                    espera.excedente().doubleValue()));
        }
    }

    /**
     * Contrato 8: una conformidad por parada atendida. Sin esta llamada Facturacion queda bloqueada
     * por FAC-01, asi que es la dependencia critica del cierre.
     */
    private void reportarAFacturacion(EjecucionDeViaje ejecucion, List<ConceptoFacturableRequest> conceptos) {
        Map<String, List<ConceptoFacturableRequest>> porOrden = conceptos.stream()
                .collect(Collectors.groupingBy(ConceptoFacturableRequest::ordenDeServicioId));

        // Los ids de las incidencias sin resolver los pone el agregado: si viniesen del cuerpo,
        // mandar la lista vacia desbloquearia FAC-05 desde fuera.
        List<String> sinResolver = ejecucion.incidenciasSinResolver().stream()
                .map(Incidencia::getId)
                .toList();

        for (Parada parada : ejecucion.paradasAtendidas()) {
            ConformidadDeEntrega conformidad = parada.getConformidad();
            List<ConceptoFacturableRemoto> deEstaOrden =
                    porOrden.getOrDefault(parada.getOrdenDeServicioId(), List.of()).stream()
                            .map(c -> new ConceptoFacturableRemoto(
                                    c.concepto(),
                                    new BigDecimal(c.monto()).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                                    c.moneda().toUpperCase(),
                                    c.detalle()))
                            .toList();

            facturacionGateway.registrarConformidad(new ConformidadPeticion(
                    ejecucion.getViajeId(),
                    parada.getOrdenDeServicioId(),
                    conformidad.getEstado().codigoDelContrato(),
                    conformidad.getFechaDeFirma(),
                    deEstaOrden,
                    sinResolver));
        }
    }

    private static Set<String> idsDeConductores(List<HorasDeConductorRequest> horas) {
        return horas.stream()
                .map(HorasDeConductorRequest::conductorId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<String> ordenesImputadas(List<ConceptoFacturableRequest> conceptos) {
        return conceptos.stream()
                .map(ConceptoFacturableRequest::ordenDeServicioId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Las paradas de una hoja de ruta son puntos de entrega y {@code Parada} no lleva tipo. Este
     * slice no se lo inventa: si algun dia hay paradas de carga, el tipo viene del contrato 4.
     */
    private static final String PUNTO_DE_ESPERA = "DESCARGA";
}
