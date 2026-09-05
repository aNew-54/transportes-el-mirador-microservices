package pe.edu.unc.elmirador.ejecucion.services;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unc.elmirador.ejecucion.dto.request.CerrarEjecucionRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.ConformidadRequest;
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
import pe.edu.unc.elmirador.ejecucion.models.vo.Evidencia;
import pe.edu.unc.elmirador.ejecucion.models.vo.ResultadoDeCheckList;
import pe.edu.unc.elmirador.ejecucion.repositories.EjecucionDeViajeRepository;

@Service
public class EjecucionDeViajeService {

    private final EjecucionDeViajeRepository repository;
    private final Clock clock;

    public EjecucionDeViajeService(EjecucionDeViajeRepository repository, Clock clock) {
        this.repository = repository;
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

        List<Parada> paradas = request.paradas().stream()
                .map(p -> new Parada(p.secuencia(), p.ordenDeServicioId(), p.direccion()))
                .toList();

        EjecucionDeViaje ejecucion = new EjecucionDeViaje(request.viajeId(), request.unidadEjecutoraId(), paradas);
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
    public EjecucionDeViajeResponse cerrar(String viajeId, CerrarEjecucionRequest request) {
        EjecucionDeViaje ejecucion = repository.findById(viajeId)
                .orElseThrow(() -> new RecursoNoEncontradoException("EjecucionDeViaje", viajeId));

        ejecucion.cerrar(request.hayLiquidacionesPendientes());
        repository.save(ejecucion);
        
        return EjecucionDeViajeMapper.mapear(ejecucion);
    }
}
