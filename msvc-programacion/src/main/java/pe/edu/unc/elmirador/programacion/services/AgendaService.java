package pe.edu.unc.elmirador.programacion.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unc.elmirador.programacion.dto.response.AgendaDeConductorResponse;
import pe.edu.unc.elmirador.programacion.dto.response.AgendaDeUnidadResponse;
import pe.edu.unc.elmirador.programacion.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.programacion.mappers.AgendaMapper;
import pe.edu.unc.elmirador.programacion.models.entity.AgendaDeConductor;
import pe.edu.unc.elmirador.programacion.models.entity.AgendaDeUnidad;
import pe.edu.unc.elmirador.programacion.repositories.AgendaDeConductorRepository;
import pe.edu.unc.elmirador.programacion.repositories.AgendaDeUnidadRepository;

@Service
public class AgendaService {

    private final AgendaDeUnidadRepository agendaDeUnidadRepository;
    private final AgendaDeConductorRepository agendaDeConductorRepository;

    public AgendaService(AgendaDeUnidadRepository agendaDeUnidadRepository,
                         AgendaDeConductorRepository agendaDeConductorRepository) {
        this.agendaDeUnidadRepository = agendaDeUnidadRepository;
        this.agendaDeConductorRepository = agendaDeConductorRepository;
    }

    @Transactional(readOnly = true)
    public AgendaDeUnidadResponse consultarAgendaDeUnidad(String unidadId) {
        AgendaDeUnidad agenda = agendaDeUnidadRepository.findById(unidadId)
                .orElseThrow(() -> new RecursoNoEncontradoException("AgendaDeUnidad", unidadId));
        return AgendaMapper.aResponse(agenda);
    }

    @Transactional(readOnly = true)
    public AgendaDeConductorResponse consultarAgendaDeConductor(String conductorId) {
        AgendaDeConductor agenda = agendaDeConductorRepository.findById(conductorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("AgendaDeConductor", conductorId));
        return AgendaMapper.aResponse(agenda);
    }
}
