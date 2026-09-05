package pe.edu.unc.elmirador.ejecucion.services;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unc.elmirador.ejecucion.dto.request.AbrirLiquidacionRequest;
import pe.edu.unc.elmirador.ejecucion.dto.request.RendirGastoRequest;
import pe.edu.unc.elmirador.ejecucion.dto.response.LiquidacionDeViajeResponse;
import pe.edu.unc.elmirador.ejecucion.exceptions.ConflictoDeRecursoException;
import pe.edu.unc.elmirador.ejecucion.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.ejecucion.mappers.LiquidacionDeViajeMapper;
import pe.edu.unc.elmirador.ejecucion.models.entity.GastoDeRuta;
import pe.edu.unc.elmirador.ejecucion.models.entity.LiquidacionDeViaje;
import pe.edu.unc.elmirador.ejecucion.models.entity.LiquidacionDeViajeId;
import pe.edu.unc.elmirador.ejecucion.models.vo.Comprobante;
import pe.edu.unc.elmirador.ejecucion.models.vo.Dinero;
import pe.edu.unc.elmirador.ejecucion.repositories.LiquidacionDeViajeRepository;

@Service
public class LiquidacionDeViajeService {

    private final LiquidacionDeViajeRepository repository;
    private final Clock clock;

    public LiquidacionDeViajeService(LiquidacionDeViajeRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public LiquidacionDeViajeResponse abrir(AbrirLiquidacionRequest request) {
        LiquidacionDeViajeId id = new LiquidacionDeViajeId(request.viajeId(), request.conductorId());
        if (repository.existsById(id)) {
            throw new ConflictoDeRecursoException("Ya existe liquidacion para el viaje " 
                    + request.viajeId() + " y conductor " + request.conductorId());
        }

        Dinero anticipo = new Dinero(request.anticipoMonto(), request.anticipoMoneda());
        LiquidacionDeViaje liquidacion = new LiquidacionDeViaje(request.viajeId(), request.conductorId(), anticipo);
        repository.save(liquidacion);

        return LiquidacionDeViajeMapper.mapear(liquidacion);
    }

    @Transactional
    public LiquidacionDeViajeResponse rendirGasto(String viajeId, String conductorId, RendirGastoRequest request) {
        LiquidacionDeViajeId id = new LiquidacionDeViajeId(viajeId, conductorId);
        LiquidacionDeViaje liquidacion = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("LiquidacionDeViaje", viajeId + "-" + conductorId));

        Comprobante comprobante = new Comprobante(request.comprobanteTipo(), request.comprobanteNumero(), request.comprobanteFecha());
        Dinero importe = new Dinero(request.importeMonto(), request.importeMoneda());
        
        GastoDeRuta gasto = new GastoDeRuta(UUID.randomUUID().toString(), request.concepto(), importe, comprobante, request.descripcion());
        liquidacion.rendirGasto(gasto);
        repository.save(liquidacion);

        return LiquidacionDeViajeMapper.mapear(liquidacion);
    }

    @Transactional
    public LiquidacionDeViajeResponse aprobar(String viajeId, String conductorId) {
        LiquidacionDeViajeId id = new LiquidacionDeViajeId(viajeId, conductorId);
        LiquidacionDeViaje liquidacion = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("LiquidacionDeViaje", viajeId + "-" + conductorId));

        liquidacion.aprobar(OffsetDateTime.now(clock));
        repository.save(liquidacion);

        return LiquidacionDeViajeMapper.mapear(liquidacion);
    }
}
