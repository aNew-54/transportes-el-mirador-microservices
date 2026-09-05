package pe.edu.unc.elmirador.unidades.services;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unc.elmirador.unidades.dto.request.AjustarInventarioRequest;
import pe.edu.unc.elmirador.unidades.dto.request.RegistrarRepuestoRequest;
import pe.edu.unc.elmirador.unidades.dto.response.RepuestoResponse;
import pe.edu.unc.elmirador.unidades.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.unidades.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.unidades.mappers.RepuestoMapper;
import pe.edu.unc.elmirador.unidades.models.entity.Repuesto;
import pe.edu.unc.elmirador.unidades.models.vo.Dinero;
import pe.edu.unc.elmirador.unidades.repositories.RepuestoRepository;

@Service
public class RepuestoService {

    private final RepuestoRepository repuestoRepository;

    public RepuestoService(RepuestoRepository repuestoRepository) {
        this.repuestoRepository = repuestoRepository;
    }

    @Transactional
    public RepuestoResponse registrar(RegistrarRepuestoRequest request) {
        if (repuestoRepository.findByCodigo(request.codigo()).isPresent()) {
            throw new ConflictoDeRecursoException("Ya existe un repuesto con el codigo " + request.codigo());
        }

        Repuesto repuesto = new Repuesto(
                UUID.randomUUID().toString(),
                request.codigo(),
                request.descripcion(),
                request.existencias(),
                request.stockMinimo(),
                new Dinero(request.costoUnitario(), request.moneda())
        );
        return RepuestoMapper.aResponse(repuestoRepository.save(repuesto));
    }

    @Transactional
    public RepuestoResponse ajustarInventario(String id, AjustarInventarioRequest request) {
        Repuesto repuesto = repuestoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Repuesto", id));

        repuesto.ajustarInventario(request.cantidad());
        return RepuestoMapper.aResponse(repuestoRepository.save(repuesto));
    }
}
