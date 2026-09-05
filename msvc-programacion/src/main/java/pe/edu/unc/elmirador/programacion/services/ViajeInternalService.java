package pe.edu.unc.elmirador.programacion.services;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.unc.elmirador.programacion.dto.internal.response.HojaDeRutaContratoResponse;
import pe.edu.unc.elmirador.programacion.dto.internal.response.ParadaContratoResponse;
import pe.edu.unc.elmirador.programacion.dto.internal.response.UbicacionContratoResponse;
import pe.edu.unc.elmirador.programacion.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.programacion.models.entity.Viaje;
import pe.edu.unc.elmirador.programacion.models.vo.AsignacionDeRecursos;
import pe.edu.unc.elmirador.programacion.models.vo.HojaDeRuta;
import pe.edu.unc.elmirador.programacion.repositories.ViajeRepository;

@Service
public class ViajeInternalService {

    private final ViajeRepository repositorio;

    public ViajeInternalService(ViajeRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(readOnly = true)
    public HojaDeRutaContratoResponse obtenerHojaDeRutaEjecutable(String viajeId) {
        Viaje viaje = repositorio.findById(viajeId)
                .orElseThrow(() -> new RecursoNoEncontradoException("viaje", viajeId));

        HojaDeRuta hoja = viaje.hojaDeRutaEjecutable();
        AsignacionDeRecursos asignacion = viaje.asignacionDeRecursos();

        List<ParadaContratoResponse> paradas = hoja.paradas().stream()
                .map(p -> new ParadaContratoResponse(
                        p.secuencia(),
                        p.tipo(),
                        p.ordenDeServicioId(),
                        p.ubicacion() == null ? null : new UbicacionContratoResponse(
                                p.ubicacion().direccion(),
                                p.ubicacion().distrito(),
                                p.ubicacion().referencia(),
                                p.ubicacion().contacto()),
                        p.horaEstimada()))
                .toList();

        return new HojaDeRutaContratoResponse(
                viaje.id(),
                viaje.estado().name(),
                asignacion != null ? asignacion.unidadId() : null,
                asignacion != null ? asignacion.conductorIds() : List.of(),
                hoja.observaciones(),
                paradas
        );
    }
}
