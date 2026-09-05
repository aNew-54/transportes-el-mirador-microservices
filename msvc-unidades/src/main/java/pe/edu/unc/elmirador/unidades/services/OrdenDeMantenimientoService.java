package pe.edu.unc.elmirador.unidades.services;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unc.elmirador.unidades.dto.request.AbrirOrdenRequest;
import pe.edu.unc.elmirador.unidades.dto.request.RegistrarTrabajoRequest;
import pe.edu.unc.elmirador.unidades.dto.response.OrdenDeMantenimientoResponse;
import pe.edu.unc.elmirador.unidades.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.unidades.mappers.OrdenDeMantenimientoMapper;
import pe.edu.unc.elmirador.unidades.models.entity.OrdenDeMantenimiento;
import pe.edu.unc.elmirador.unidades.models.entity.TrabajoRealizado;
import pe.edu.unc.elmirador.unidades.models.entity.Unidad;
import pe.edu.unc.elmirador.unidades.models.vo.Dinero;
import pe.edu.unc.elmirador.unidades.models.vo.Kilometraje;
import pe.edu.unc.elmirador.unidades.repositories.OrdenDeMantenimientoRepository;
import pe.edu.unc.elmirador.unidades.repositories.UnidadRepository;

@Service
public class OrdenDeMantenimientoService {

    private final OrdenDeMantenimientoRepository ordenRepository;
    private final UnidadRepository unidadRepository;
    private final Clock reloj;

    public OrdenDeMantenimientoService(OrdenDeMantenimientoRepository ordenRepository,
                                       UnidadRepository unidadRepository, Clock reloj) {
        this.ordenRepository = ordenRepository;
        this.unidadRepository = unidadRepository;
        this.reloj = reloj;
    }

    @Transactional
    public OrdenDeMantenimientoResponse abrir(AbrirOrdenRequest request) {
        Unidad unidad = unidadRepository.findById(request.unidadId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Unidad", request.unidadId()));

        OrdenDeMantenimiento orden = OrdenDeMantenimiento.abrir(
                UUID.randomUUID().toString(),
                unidad.getId(),
                request.tipoMantenimiento(),
                new Kilometraje(request.kilometrajeAtencion()),
                unidad.getProgramaDeMantenimiento().kmUltimoServicio(),
                LocalDate.now(reloj),
                request.moneda()
        );

        return OrdenDeMantenimientoMapper.aResponse(ordenRepository.save(orden));
    }

    @Transactional
    public OrdenDeMantenimientoResponse registrarTrabajo(String id, RegistrarTrabajoRequest request) {
        OrdenDeMantenimiento orden = buscarOrden(id);

        Dinero costoManoDeObra = new Dinero(request.costoManoDeObra(), request.monedaManoDeObra());
        TrabajoRealizado trabajo = new TrabajoRealizado(
                UUID.randomUUID().toString(),
                request.descripcion(),
                costoManoDeObra,
                request.repuestoId(),
                request.cantidadRepuesto() != null ? request.cantidadRepuesto() : 0
        );

        orden.registrarTrabajo(trabajo);
        return OrdenDeMantenimientoMapper.aResponse(ordenRepository.save(orden));
    }

    @Transactional
    public OrdenDeMantenimientoResponse cerrar(String id) {
        OrdenDeMantenimiento orden = buscarOrden(id);
        orden.cerrar(LocalDate.now(reloj));

        // Update the unit's maintenance program
        Unidad unidad = unidadRepository.findById(orden.getUnidadId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Unidad", orden.getUnidadId()));
        unidad.registrarMantenimientoRealizado(orden.getKmAtencion());
        unidadRepository.save(unidad);

        return OrdenDeMantenimientoMapper.aResponse(ordenRepository.save(orden));
    }

    private OrdenDeMantenimiento buscarOrden(String id) {
        return ordenRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("OrdenDeMantenimiento", id));
    }
}
