package pe.edu.unc.elmirador.programacion.services;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unc.elmirador.programacion.dto.request.AsignarRecursosRequest;
import pe.edu.unc.elmirador.programacion.dto.request.CapacidadRequest;
import pe.edu.unc.elmirador.programacion.dto.request.CargaRequest;
import pe.edu.unc.elmirador.programacion.dto.request.ClausulaDeConsolidacionRequest;
import pe.edu.unc.elmirador.programacion.dto.request.ConsolidarOrdenRequest;
import pe.edu.unc.elmirador.programacion.dto.request.ElegibilidadDeRecursoRequest;
import pe.edu.unc.elmirador.programacion.dto.request.ParadaRequest;
import pe.edu.unc.elmirador.programacion.dto.request.PlanificarViajeRequest;
import pe.edu.unc.elmirador.programacion.dto.request.ProgramarViajeRequest;
import pe.edu.unc.elmirador.programacion.dto.request.RutaRequest;
import pe.edu.unc.elmirador.programacion.dto.request.VentanaDeTiempoRequest;
import pe.edu.unc.elmirador.programacion.dto.response.ViajeResponse;
import pe.edu.unc.elmirador.programacion.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.programacion.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.programacion.mappers.ViajeMapper;
import pe.edu.unc.elmirador.programacion.models.entity.AgendaDeConductor;
import pe.edu.unc.elmirador.programacion.models.entity.AgendaDeUnidad;
import pe.edu.unc.elmirador.programacion.models.entity.Viaje;
import pe.edu.unc.elmirador.programacion.models.vo.AsignacionDeRecursos;
import pe.edu.unc.elmirador.programacion.models.vo.Capacidad;
import pe.edu.unc.elmirador.programacion.models.vo.Carga;
import pe.edu.unc.elmirador.programacion.models.vo.ClausulaDeConsolidacion;
import pe.edu.unc.elmirador.programacion.models.vo.ElegibilidadDeRecurso;
import pe.edu.unc.elmirador.programacion.models.vo.HojaDeRuta;
import pe.edu.unc.elmirador.programacion.models.vo.Parada;
import pe.edu.unc.elmirador.programacion.models.vo.Ruta;
import pe.edu.unc.elmirador.programacion.models.vo.VentanaDeTiempo;
import pe.edu.unc.elmirador.programacion.repositories.AgendaDeConductorRepository;
import pe.edu.unc.elmirador.programacion.repositories.AgendaDeUnidadRepository;
import pe.edu.unc.elmirador.programacion.repositories.ViajeRepository;

@Service
public class ViajeService {

    private final ViajeRepository viajeRepository;
    private final AgendaDeUnidadRepository agendaDeUnidadRepository;
    private final AgendaDeConductorRepository agendaDeConductorRepository;

    public ViajeService(ViajeRepository viajeRepository,
                        AgendaDeUnidadRepository agendaDeUnidadRepository,
                        AgendaDeConductorRepository agendaDeConductorRepository) {
        this.viajeRepository = viajeRepository;
        this.agendaDeUnidadRepository = agendaDeUnidadRepository;
        this.agendaDeConductorRepository = agendaDeConductorRepository;
    }

    @Transactional(readOnly = true)
    public ViajeResponse consultar(String id) {
        Viaje viaje = viajeRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Viaje", id));
        return ViajeMapper.aResponse(viaje);
    }

    @Transactional
    public ViajeResponse planificar(PlanificarViajeRequest peticion) {
        if (viajeRepository.existsById(peticion.id())) {
            throw new ConflictoDeRecursoException("Ya existe un viaje con id " + peticion.id());
        }

        Ruta ruta = toRuta(peticion.ruta());
        VentanaDeTiempo ventana = toVentanaDeTiempo(peticion.ventana());
        Carga carga = toCarga(peticion.cargaInicial());

        Viaje viaje = Viaje.planificar(peticion.id(), ruta, ventana, carga);
        viajeRepository.save(viaje);
        return ViajeMapper.aResponse(viaje);
    }

    @Transactional
    public ViajeResponse consolidarOrden(String id, ConsolidarOrdenRequest peticion) {
        Viaje viaje = viajeRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Viaje", id));

        Carga carga = toCarga(peticion.carga());
        Ruta ruta = toRuta(peticion.rutaDeLaOrden());
        VentanaDeTiempo ventana = toVentanaDeTiempo(peticion.ventanaDeLaOrden());
        ClausulaDeConsolidacion clausula = toClausulaDeConsolidacion(peticion.clausulaDelContrato());
        Capacidad capacidad = toCapacidad(peticion.capacidadDeLaUnidad());

        viaje.consolidarOrden(carga, ruta, ventana, clausula, capacidad);
        viajeRepository.save(viaje);
        return ViajeMapper.aResponse(viaje);
    }

    @Transactional
    public ViajeResponse asignarRecursos(String id, AsignarRecursosRequest peticion) {
        Viaje viaje = viajeRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Viaje", id));

        AgendaDeUnidad agendaUnidad = agendaDeUnidadRepository.findById(peticion.unidadId())
                .orElseGet(() -> new AgendaDeUnidad(peticion.unidadId()));
        agendaUnidad.reservar(
                UUID.randomUUID().toString(),
                viaje.ventana(),
                toElegibilidadDeRecurso(peticion.elegibilidadDeLaUnidad()),
                viaje.id()
        );
        agendaDeUnidadRepository.save(agendaUnidad);

        for (int i = 0; i < peticion.conductorIds().size(); i++) {
            String conductorId = peticion.conductorIds().get(i);
            ElegibilidadDeRecurso elegibilidad = toElegibilidadDeRecurso(peticion.elegibilidadDeLosConductores().get(i));
            AgendaDeConductor agendaConductor = agendaDeConductorRepository.findById(conductorId)
                    .orElseGet(() -> new AgendaDeConductor(conductorId));
            agendaConductor.reservar(
                    UUID.randomUUID().toString(),
                    viaje.ventana(),
                    elegibilidad,
                    viaje.id()
            );
            agendaDeConductorRepository.save(agendaConductor);
        }

        AsignacionDeRecursos asignacion = new AsignacionDeRecursos(
                peticion.unidadId(),
                peticion.conductorIds(),
                peticion.conRelevo()
        );
        viaje.asignarRecursos(asignacion);
        viajeRepository.save(viaje);
        return ViajeMapper.aResponse(viaje);
    }

    @Transactional
    public ViajeResponse programar(String id, ProgramarViajeRequest peticion) {
        Viaje viaje = viajeRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Viaje", id));

        List<Parada> paradas = peticion.hojaDeRuta().stream()
                .map(this::toParada)
                .toList();
        HojaDeRuta hojaDeRuta = new HojaDeRuta(paradas);

        viaje.confirmarProgramacion(hojaDeRuta);
        
        // Confirmar reservas
        if (viaje.asignacionDeRecursos() != null) {
            String unidadId = viaje.asignacionDeRecursos().unidadId();
            if (unidadId != null) {
                agendaDeUnidadRepository.findById(unidadId).ifPresent(agenda -> {
                    agenda.reservas().stream()
                            .filter(r -> r.viajeId().equals(viaje.id()) && r.estado().bloqueaElRecurso())
                            .forEach(r -> agenda.confirmar(r.id()));
                    agendaDeUnidadRepository.save(agenda);
                });
            }
            for (String conductorId : viaje.asignacionDeRecursos().conductorIds()) {
                agendaDeConductorRepository.findById(conductorId).ifPresent(agenda -> {
                    agenda.reservas().stream()
                            .filter(r -> r.viajeId().equals(viaje.id()) && r.estado().bloqueaElRecurso())
                            .forEach(r -> agenda.confirmar(r.id()));
                    agendaDeConductorRepository.save(agenda);
                });
            }
        }
        
        viajeRepository.save(viaje);
        return ViajeMapper.aResponse(viaje);
    }

    @Transactional
    public ViajeResponse despachar(String id) {
        Viaje viaje = viajeRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Viaje", id));
        viaje.autorizarDespacho();
        viajeRepository.save(viaje);
        return ViajeMapper.aResponse(viaje);
    }

    @Transactional
    public ViajeResponse cancelar(String id) {
        Viaje viaje = viajeRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Viaje", id));
        viaje.cancelar();

        if (viaje.asignacionDeRecursos() != null) {
            String unidadId = viaje.asignacionDeRecursos().unidadId();
            if (unidadId != null) {
                agendaDeUnidadRepository.findById(unidadId).ifPresent(agenda -> {
                    agenda.reservas().stream()
                            .filter(r -> r.viajeId().equals(viaje.id()) && r.estado().bloqueaElRecurso())
                            .forEach(r -> agenda.liberar(r.id()));
                    agendaDeUnidadRepository.save(agenda);
                });
            }
            for (String conductorId : viaje.asignacionDeRecursos().conductorIds()) {
                agendaDeConductorRepository.findById(conductorId).ifPresent(agenda -> {
                    agenda.reservas().stream()
                            .filter(r -> r.viajeId().equals(viaje.id()) && r.estado().bloqueaElRecurso())
                            .forEach(r -> agenda.liberar(r.id()));
                    agendaDeConductorRepository.save(agenda);
                });
            }
        }

        viajeRepository.save(viaje);
        return ViajeMapper.aResponse(viaje);
    }

    private Ruta toRuta(RutaRequest peticion) {
        return new Ruta(peticion.origen(), peticion.destino(), peticion.corredor());
    }

    private VentanaDeTiempo toVentanaDeTiempo(VentanaDeTiempoRequest peticion) {
        return new VentanaDeTiempo(peticion.desde(), peticion.hasta());
    }

    private Carga toCarga(CargaRequest peticion) {
        return new Carga(
                peticion.ordenDeServicioId(),
                peticion.pesoKg(),
                peticion.volumenM3(),
                peticion.tipo(),
                peticion.secuenciaDeDescarga()
        );
    }

    private ClausulaDeConsolidacion toClausulaDeConsolidacion(ClausulaDeConsolidacionRequest peticion) {
        return new ClausulaDeConsolidacion(peticion.permitida(), peticion.restricciones());
    }

    private Capacidad toCapacidad(CapacidadRequest peticion) {
        return new Capacidad(peticion.pesoMaximoKg(), peticion.volumenMaximoM3());
    }

    private ElegibilidadDeRecurso toElegibilidadDeRecurso(ElegibilidadDeRecursoRequest peticion) {
        return new ElegibilidadDeRecurso(peticion.elegible(), peticion.motivos());
    }

    private Parada toParada(ParadaRequest peticion) {
        return new Parada(
                peticion.secuencia(),
                peticion.tipo(),
                peticion.ordenDeServicioId(),
                peticion.ubicacion(),
                peticion.horaEstimada()
        );
    }
}
