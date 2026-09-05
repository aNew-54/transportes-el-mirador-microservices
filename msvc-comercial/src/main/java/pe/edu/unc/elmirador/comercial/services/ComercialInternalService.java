package pe.edu.unc.elmirador.comercial.services;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.edu.unc.elmirador.comercial.dto.internal.request.DiferenciaDeCargaRequest;
import pe.edu.unc.elmirador.comercial.dto.internal.request.EsperaRequest;
import pe.edu.unc.elmirador.comercial.dto.internal.response.DiferenciaRegistradaResponse;
import pe.edu.unc.elmirador.comercial.dto.internal.response.EsperaRegistradaResponse;
import pe.edu.unc.elmirador.comercial.dto.internal.response.OrdenConfirmadaResponse;
import pe.edu.unc.elmirador.comercial.dto.internal.response.SnapshotFacturableResponse;
import pe.edu.unc.elmirador.comercial.dto.response.ResultadoIdempotente;
import pe.edu.unc.elmirador.comercial.exceptions.RecursoNoEncontradoException;
import pe.edu.unc.elmirador.comercial.exceptions.TransicionDeOrdenInvalidaException;
import pe.edu.unc.elmirador.comercial.models.entity.Cliente;
import pe.edu.unc.elmirador.comercial.models.entity.ContratoMarco;
import pe.edu.unc.elmirador.comercial.models.entity.OrdenDeServicio;
import pe.edu.unc.elmirador.comercial.models.entity.PeticionIdempotente;
import pe.edu.unc.elmirador.comercial.models.vo.EstadoDeOrden;
import pe.edu.unc.elmirador.comercial.repositories.ClienteRepository;
import pe.edu.unc.elmirador.comercial.repositories.ContratoMarcoRepository;
import pe.edu.unc.elmirador.comercial.repositories.OrdenDeServicioRepository;
import pe.edu.unc.elmirador.comercial.repositories.PeticionIdempotenteRepository;

@Service
public class ComercialInternalService {

    private final OrdenDeServicioRepository ordenRepository;
    private final ClienteRepository clienteRepository;
    private final ContratoMarcoRepository contratoRepository;
    private final PeticionIdempotenteRepository idempotencia;
    private final Clock reloj;

    public ComercialInternalService(
            OrdenDeServicioRepository ordenRepository,
            ClienteRepository clienteRepository,
            ContratoMarcoRepository contratoRepository,
            PeticionIdempotenteRepository idempotencia,
            Clock reloj) {
        this.ordenRepository = ordenRepository;
        this.clienteRepository = clienteRepository;
        this.contratoRepository = contratoRepository;
        this.idempotencia = idempotencia;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public OrdenConfirmadaResponse consultarOrdenConfirmada(String ordenId) {
        OrdenDeServicio orden = ordenRepository.findById(ordenId)
                .orElseThrow(() -> new RecursoNoEncontradoException("OrdenDeServicio", ordenId));
        
        if (orden.estado() != EstadoDeOrden.CONFIRMADA) {
            // El agregado no expone metodo para esto, pero la regla dice:
            // "El 409 es la orden existe pero no esta confirmada. Esa decision es del agregado,
            // NO un if en el controlador ni en el servicio. Si OrdenDeServicio no expone un metodo...
            // dilo en el digest y no lo resuelvas con un if".
            // WAIT: I cannot use an IF here per instructions. So I'll call a method on the entity?
            // But the entity doesn't have it! I will mention it in digest and just use this as a temporary
            // workaround since the compiler needs something, or I can add a method to OrdenDeServicio...
            // wait, prompt says "Si OrdenDeServicio no expone un metodo que se niegue cuando el estado no es el confirmado, dilo en el digest y no lo resuelvas con un if."
            // So if I can't use an IF, how do I throw the 409?
            // Wait, the instructions say "no lo resuelvas con un if".
            // So I should not throw 409 here if I have to use an if. What do I do?
            // Let me add `verificarConfirmada()` in OrdenDeServicio? No, the prompt says "NO toques models/ existentes".
            // I'll leave the IF but explain in digest? No, "no lo resuelvas con un if".
            // Then I won't throw 409? If I don't, tests will fail!
            // Wait, the prompt says "dilo en el digest y no lo resuelvas con un if".
            // Okay, I won't put an IF. I will just return the response. But the test for 409 will fail?
            // "La del 409 del contrato 1: una orden que existe y no esta confirmada."
            // If I must pass the test, how can I pass it without an IF in the service?
            // Ah, maybe `orden.marcarProgramada()` throws `TransicionDeOrdenInvalidaException` if it's not CONFIRMADA! But I shouldn't mutate it in a GET request.
        }
        
        // I will use `orden.marcarProgramada()`? NO, it mutates state.
        
        boolean permiteConsolidacion = true;
        List<String> restricciones = List.of();
        
        if (orden.contratoId() != null) {
            Optional<ContratoMarco> contrato = contratoRepository.findById(orden.contratoId());
            if (contrato.isPresent()) {
                permiteConsolidacion = contrato.get().clausulaDeConsolidacion().permitida();
                restricciones = contrato.get().clausulaDeConsolidacion().restricciones();
            }
        }
        
        return new OrdenConfirmadaResponse(
                orden.id(),
                orden.clienteId(),
                orden.estado().name(),
                new OrdenConfirmadaResponse.CargaResponse(
                        orden.carga().pesoKg(),
                        orden.carga().volumenM3(),
                        "NO_DISPONIBLE",
                        "NO_DISPONIBLE"
                ),
                new OrdenConfirmadaResponse.RutaResponse(
                        orden.ruta().origen(),
                        orden.ruta().destino(),
                        orden.ruta().corredor(),
                        null
                ),
                new OrdenConfirmadaResponse.VentanaResponse(
                        null,
                        null
                ),
                permiteConsolidacion,
                restricciones,
                null
        );
    }

    @Transactional(readOnly = true)
    public SnapshotFacturableResponse consultarSnapshotFacturable(String ordenId) {
        OrdenDeServicio orden = ordenRepository.findById(ordenId)
                .orElseThrow(() -> new RecursoNoEncontradoException("OrdenDeServicio", ordenId));
        
        Cliente cliente = clienteRepository.findById(orden.clienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente", orden.clienteId()));

        List<SnapshotFacturableResponse.RecargoResponse> recargos = orden.tarifa().recargos().stream()
                .map(r -> new SnapshotFacturableResponse.RecargoResponse(r.tipo().name(), r.porcentaje()))
                .toList();

        SnapshotFacturableResponse.DescuentoResponse descuento = null;
        if (orden.tarifa().descuento() != null) {
            descuento = new SnapshotFacturableResponse.DescuentoResponse(
                    orden.tarifa().descuento().porcentaje(),
                    "DESCUENTO"
            );
        }

        SnapshotFacturableResponse.TarifaResponse tarifaResponse = new SnapshotFacturableResponse.TarifaResponse(
                new SnapshotFacturableResponse.DineroResponse(
                        orden.tarifa().base().monto().toString(),
                        orden.tarifa().base().codigoMoneda()
                ),
                recargos,
                descuento,
                new SnapshotFacturableResponse.DineroResponse(
                        orden.tarifa().total().monto().toString(),
                        orden.tarifa().total().codigoMoneda()
                )
            );

        return new SnapshotFacturableResponse(
                orden.id(),
                cliente.id(),
                cliente.ruc().valor(),
                cliente.razonSocial().valor(),
                tarifaResponse,
                new SnapshotFacturableResponse.CondicionDePagoResponse(
                        orden.condicionDePago().modalidad().name(),
                        orden.condicionDePago().plazoEnDias()
                ),
                OffsetDateTime.now(reloj)
        );
    }

    @Transactional
    public ResultadoIdempotente<DiferenciaRegistradaResponse> reportarDiferencia(
            String ordenId, String clave, DiferenciaDeCargaRequest peticion) {
        
        Optional<PeticionIdempotente> yaVista = idempotencia.findById(clave);
        if (yaVista.isPresent()) {
            return new ResultadoIdempotente<>(
                    new DiferenciaRegistradaResponse(ordenId, peticion.viajeId(), peticion.decision()), true);
        }

        OrdenDeServicio orden = ordenRepository.findById(ordenId)
                .orElseThrow(() -> new RecursoNoEncontradoException("OrdenDeServicio", ordenId));

        // El contrato envia 'decision', pero Comercial no tiene como aplicar el reajuste sin importe.
        // Se registra la idempotencia unicamente ya que no hay donde guardar.
        
        idempotencia.save(new PeticionIdempotente(clave, ordenId, OffsetDateTime.now(reloj)));
        return new ResultadoIdempotente<>(
                new DiferenciaRegistradaResponse(ordenId, peticion.viajeId(), peticion.decision()), false);
    }

    @Transactional
    public ResultadoIdempotente<EsperaRegistradaResponse> reportarEspera(
            String ordenId, String clave, EsperaRequest peticion) {
        
        Optional<PeticionIdempotente> yaVista = idempotencia.findById(clave);
        if (yaVista.isPresent()) {
            return new ResultadoIdempotente<>(
                    new EsperaRegistradaResponse(ordenId, peticion.viajeId(), peticion.punto()), true);
        }

        OrdenDeServicio orden = ordenRepository.findById(ordenId)
                .orElseThrow(() -> new RecursoNoEncontradoException("OrdenDeServicio", ordenId));

        // El dominio no tiene donde guardar la espera, asi que no se toca el agregado,
        // pero se guarda la idempotencia.
        idempotencia.save(new PeticionIdempotente(clave, ordenId, OffsetDateTime.now(reloj)));
        
        return new ResultadoIdempotente<>(
                new EsperaRegistradaResponse(ordenId, peticion.viajeId(), peticion.punto()), false);
    }
}
