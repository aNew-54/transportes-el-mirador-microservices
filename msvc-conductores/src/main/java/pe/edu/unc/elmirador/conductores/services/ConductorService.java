package pe.edu.unc.elmirador.conductores.services;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unc.elmirador.conductores.dto.request.RegistrarConductorRequest;
import pe.edu.unc.elmirador.conductores.dto.request.RegistrarInduccionRequest;
import pe.edu.unc.elmirador.conductores.dto.request.RenovarLicenciaRequest;
import pe.edu.unc.elmirador.conductores.dto.request.SuspenderConductorRequest;
import pe.edu.unc.elmirador.conductores.dto.response.AlertaResponse;
import pe.edu.unc.elmirador.conductores.dto.response.ConductorResponse;
import pe.edu.unc.elmirador.conductores.dto.response.HorasResponse;
import pe.edu.unc.elmirador.conductores.dto.response.InduccionResponse;
import pe.edu.unc.elmirador.conductores.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.conductores.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.conductores.mappers.ConductorMapper;
import pe.edu.unc.elmirador.conductores.models.entity.Conductor;
import pe.edu.unc.elmirador.conductores.models.entity.Induccion;
import pe.edu.unc.elmirador.conductores.models.vo.CategoriaDeLicencia;
import pe.edu.unc.elmirador.conductores.models.vo.EstadoDeHabilitacion;
import pe.edu.unc.elmirador.conductores.models.vo.HorasDeConduccion;
import pe.edu.unc.elmirador.conductores.models.vo.NumeroDeLicencia;
import pe.edu.unc.elmirador.conductores.models.vo.PeriodoDeVigencia;
import pe.edu.unc.elmirador.conductores.models.vo.SituacionDeHabilitacion;
import pe.edu.unc.elmirador.conductores.repositories.ConductorRepository;

/**
 * Servicio de aplicacion del agregado {@code Conductor}.
 *
 * <p>Orquesta y transacciona; no decide reglas de negocio. Las unicas dos comprobaciones que hace
 * son la existencia y la unicidad, y ninguna cabe dentro del agregado porque las dos exigen mirar al
 * repositorio. Cualquier otra condicion pertenece al objeto de valor o al agregado, y esta ahi.
 *
 * <p>El reloj llega inyectado (regla D1). Este servicio nunca llama a {@code LocalDate.now()} sin el.
 */
@Service
public class ConductorService {

    private final ConductorRepository repositorio;
    private final Clock reloj;

    public ConductorService(ConductorRepository repositorio, Clock reloj) {
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    @Transactional
    public ConductorResponse registrar(RegistrarConductorRequest peticion) {
        NumeroDeLicencia licencia = new NumeroDeLicencia(peticion.numeroDeLicencia());

        // Unicidad: no cabe en el agregado porque exige mirar a los demas conductores.
        if (repositorio.findByNumeroDeLicenciaValor(licencia.valor()).isPresent()) {
            throw new ConflictoDeRecursoException(
                    "Ya existe un conductor con el numero de licencia " + licencia.valor());
        }

        LocalDate hoy = LocalDate.now(reloj);
        Conductor conductor = new Conductor(
                UUID.randomUUID().toString(),
                peticion.nombreCompleto(),
                licencia,
                peticion.categoriaDeLicencia(),
                new PeriodoDeVigencia(peticion.licenciaDesde(), peticion.licenciaHasta()),
                HorasDeConduccion.ventanaDe(hoy),
                EstadoDeHabilitacion.habilitado(),
                List.of()
        );

        return ConductorMapper.aRespuesta(repositorio.save(conductor));
    }

    @Transactional(readOnly = true)
    public ConductorResponse porId(String id) {
        return ConductorMapper.aRespuesta(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<ConductorResponse> listar(SituacionDeHabilitacion situacion) {
        List<Conductor> conductores = situacion == null
                ? repositorio.findAll()
                : repositorio.findByEstadoSituacion(situacion);
        return conductores.stream().map(ConductorMapper::aRespuesta).toList();
    }

    @Transactional
    public ConductorResponse renovarLicencia(String id, RenovarLicenciaRequest peticion) {
        Conductor conductor = buscar(id);
        conductor.renovarLicencia(
                new NumeroDeLicencia(peticion.numeroDeLicencia()),
                peticion.categoriaDeLicencia(),
                new PeriodoDeVigencia(peticion.vigenteDesde(), peticion.vigenteHasta())
        );
        return ConductorMapper.aRespuesta(repositorio.save(conductor));
    }

    @Transactional
    public InduccionResponse registrarInduccion(String id, RegistrarInduccionRequest peticion) {
        Conductor conductor = buscar(id);
        Induccion induccion = new Induccion(
                UUID.randomUUID().toString(),
                peticion.clienteId(),
                new PeriodoDeVigencia(peticion.vigenteDesde(), peticion.vigenteHasta())
        );
        conductor.registrarInduccion(induccion);
        repositorio.save(conductor);
        return ConductorMapper.aRespuesta(induccion);
    }

    @Transactional(readOnly = true)
    public HorasResponse horas(String id) {
        return ConductorMapper.aHoras(buscar(id));
    }

    @Transactional
    public ConductorResponse registrarDescanso(String id) {
        Conductor conductor = buscar(id);
        conductor.registrarDescanso(LocalDate.now(reloj));
        return ConductorMapper.aRespuesta(repositorio.save(conductor));
    }

    @Transactional
    public ConductorResponse suspender(String id, SuspenderConductorRequest peticion) {
        Conductor conductor = buscar(id);
        conductor.suspender(peticion.motivo());
        return ConductorMapper.aRespuesta(repositorio.save(conductor));
    }

    /**
     * Rehabilita al conductor. Si su licencia no esta vigente hoy, el agregado lo impide con
     * {@code RehabilitacionInvalidaException} y el manejador lo traduce a {@code 409}: es un «ahora
     * no», porque la misma peticion funcionaria con la licencia renovada.
     */
    @Transactional
    public ConductorResponse rehabilitar(String id) {
        Conductor conductor = buscar(id);
        conductor.rehabilitar(LocalDate.now(reloj));
        return ConductorMapper.aRespuesta(repositorio.save(conductor));
    }

    @Transactional(readOnly = true)
    public List<AlertaResponse> alertas(int dias) {
        LocalDate hoy = LocalDate.now(reloj);
        return repositorio.findAll().stream()
                .flatMap(conductor -> ConductorMapper.alertasDe(conductor, dias, hoy).stream())
                .toList();
    }

    private Conductor buscar(String id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("conductor", id));
    }
}
